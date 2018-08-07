/**
 * Copyright (c) 2018, SequoiaDB Ltd.
 * File Name:ScmOperator.java
 * 类的详细描述
 *
 *  @author 类创建者姓名
 * Date:2018-7-29上午11:19:09
 *  @version 1.00
 */
package com.scm.perftest ;

import java.io.ByteArrayInputStream ;
import java.io.ByteArrayOutputStream ;
import java.io.InputStream ;
import java.net.InetAddress ;
import java.net.UnknownHostException ;
import java.util.ArrayList ;
import java.util.Calendar ;
import java.util.Date ;
import java.util.List ;
import java.util.Properties ;
import java.util.Random ;
import java.util.concurrent.atomic.AtomicLong ;

import org.bson.BSONObject ;
import org.bson.BasicBSONObject ;

import com.sequoiacm.client.common.ScmType.ScopeType ;
import com.sequoiacm.client.core.ScmBatch ;
import com.sequoiacm.client.core.ScmBreakpointFile ;
import com.sequoiacm.client.core.ScmConfigOption ;
import com.sequoiacm.client.core.ScmCursor ;
import com.sequoiacm.client.core.ScmDirectory ;
import com.sequoiacm.client.core.ScmFactory ;
import com.sequoiacm.client.core.ScmFile ;
import com.sequoiacm.client.core.ScmSession ;
import com.sequoiacm.client.core.ScmWorkspace ;
import com.sequoiacm.client.element.ScmFileBasicInfo ;
import com.sequoiacm.client.element.ScmId ;
import com.sequoiacm.client.exception.ScmException ;
import com.sequoiacm.common.MimeType ;
import com.sequoiadb.base.CollectionSpace ;
import com.sequoiadb.base.DBCollection ;
import com.sequoiadb.base.DBCursor ;
import com.sequoiadb.base.Sequoiadb ;

public class ScmOperator {
    private final static int size200k = 200 * 1024 ;
    private final static int size1m = 1024 * 1024 ;
    private final static int size10m = 10 * 1024 * 1024 ;
    private final static int size100m = 100 * 1024 * 1024 ;
    private final static int size100k = 100 * 1024 ;

    private static byte[] d100m = new byte[ size100m ] ;
    private static List< ScmId > scmIdSet = new ArrayList< ScmId >();
    private static List< ScmId > batchIdSet = new ArrayList< ScmId >() ;
    private static AtomicLong sequence = new AtomicLong( 0 ) ;
    private static Random rand = new Random() ;
    private static List< String > dirNames = new ArrayList< String >() ;
    private ScmSession session = null ;
    private ScmWorkspace ws = null ;
    private String wsName = null ;
    private int dirSelector = 0 ;

    private String dsUrl = null ;
    private String dsUser = null ;
    private String dsPwd = null ;
    private int fileSize = size100k ;
    private int count = 0 ;
    private ScmDirectory curDir ;
    private int fileNumPerDir = 1000 ;
    private static InetAddress localAddr = null ;

    static {
        try {
            localAddr = InetAddress.getLocalHost() ;
        } catch ( UnknownHostException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace() ;
        }
    }

    public ScmOperator() {
    }

    private void initScmIds( Sequoiadb db, int fileSize ) throws ScmException {
        if ( !scmIdSet.isEmpty() )
            return ;
        synchronized ( scmIdSet ) {
            if ( !scmIdSet.isEmpty() )
                return ;
            String csName = String.format( "%s_META", this.wsName ) ;
            String clName = null ;
            clName = "FILE_2018" ;
            CollectionSpace cs = db.getCollectionSpace( csName ) ;
            DBCollection cl = cs.getCollection( clName ) ;

            BasicBSONObject matcher = null ;
            if ( fileSize > 0 ) {
                matcher = new BasicBSONObject() ;
                matcher.put( "size", fileSize ) ;
            }
            DBCursor cursor = cl.query( matcher, null, null, null ) ;
            while ( cursor.hasNext() ) {
                BasicBSONObject doc = ( BasicBSONObject ) cursor.getNext() ;
                scmIdSet.add( new ScmId( doc.getString( "id" ) ) ) ;
            }
            cursor.close() ;
        }
    }

    public void initBatchIds( Sequoiadb db )
            throws ScmException {
        if ( !batchIdSet.isEmpty() )
            return ;
        synchronized ( rand ) {
            if ( !batchIdSet.isEmpty() )
                return ;
            String csName = String.format( "%s_META", this.wsName ) ;
            String clName = "BATCH" ;

            CollectionSpace cs = db.getCollectionSpace( csName ) ;
            DBCollection cl = cs.getCollection( clName ) ;
            BSONObject matcher = null ;
            DBCursor cursor = cl.query( matcher, null, null, null ) ;
            while ( cursor.hasNext() ) {
                BasicBSONObject doc = ( BasicBSONObject ) cursor.getNext() ;
                batchIdSet.add( new ScmId( doc.getString( "id" ) ) ) ;
            }
            cursor.close() ;
        }

    }

    private void load( Sequoiadb db, int fileSize,
            boolean isNeedQueyBatch ) throws ScmException {
        if ( fileSize != 0 ) {
            db = db != null ? db : new Sequoiadb( this.dsUrl, this.dsUser,
                    this.dsPwd ) ;
            initScmIds( db, fileSize ) ;
        }

        if ( isNeedQueyBatch ) {
            db = db != null ? db : new Sequoiadb( this.dsUrl, this.dsUser,
                    this.dsPwd ) ;
            initBatchIds( db ) ;
        }
    }

    private void loadDirectory( Sequoiadb db, String wsName ) {
        if ( !dirNames.isEmpty() )
            return ;
        synchronized ( dirNames ) {
            if ( !dirNames.isEmpty() )
                return ;
            String csName = String.format( "%s_META", wsName ) ;
            CollectionSpace cs = db.getCollectionSpace( csName ) ;
            DBCollection cl = cs.getCollection( "DIRECTORY" ) ;

            BasicBSONObject obj = new BasicBSONObject() ;
            obj.put( "parent_directory_id", "000000000000000000000000" ) ;
            DBCursor cursor = cl.query( obj, null, null, null ) ;
            while ( cursor.hasNext() ) {
                BasicBSONObject ret = ( BasicBSONObject ) cursor.getNext() ;
                dirNames.add( ret.getString( "name" ) ) ;
            }
            cursor.close() ;
        }
    }

    public void init( Properties prop ) throws ScmException {
        String url = prop.getProperty( Common.GATEWAYURL ) ;
        String user = prop.getProperty( Common.USR ) ;
        String password = prop.getProperty( Common.PWD ) ;
        
        this.dsUrl = prop.getProperty( Common.DSURL ) ;
        this.dsUser = prop.getProperty( Common.DSUSR ) ;
        this.dsPwd = prop.getProperty( Common.DSPWD ) ;
        
        this.fileNumPerDir = Integer.parseInt( prop.getProperty(
                Common.FILENUMPERDIR, "1000" ) ) ;
        this.fileSize = Integer.parseInt( prop.getProperty( Common.FILESIZE,
                "0" ) ) ;
        
        this.wsName = prop.getProperty( Common.WSNAME ) ;
        boolean isNeedQueyBatch = Boolean.parseBoolean( prop.getProperty(
                Common.ISNEEDLOADBATCH, "false" ) ) ;
        
        ScmConfigOption opt = new ScmConfigOption() ;
        opt.setUser( user ) ;
        opt.setPasswd( password ) ;
        String[] urls = url.split( "," ) ;

        for ( String urlItem : urls ) {
            opt.addUrl( urlItem ) ;
        }

        Sequoiadb db = null ;
        if ( dirNames.isEmpty() ) {
            db = new Sequoiadb( this.dsUrl, this.dsUser, this.dsPwd ) ;
            loadDirectory( db, wsName ) ;
        }

        if ( this.fileSize == 0 && isNeedQueyBatch == false ) {
            session = ScmFactory.Session.createSession( opt ) ;
            ws = ScmFactory.Workspace.getWorkspace( wsName, session ) ;
            return ;
        } else {
            load( db, fileSize, isNeedQueyBatch ) ;
            session = ScmFactory.Session.createSession( opt ) ;
            ws = ScmFactory.Workspace.getWorkspace( wsName, session ) ;
        }

        if ( db != null ) {
            db.close() ;
        }
    }

    public ScmId getFileId( int pos ) {
        return scmIdSet.get( pos % scmIdSet.size() ) ;
    }

    private InputStream selectFile( int fileSize ) {
        InputStream in = null ;
        in = new ByteArrayInputStream( d100m, 0, fileSize ) ;
        return in ;
    }

    private String selectFileName() {
         String fileName = String.format( "%s_%d%04d",
         localAddr.getHostName(),
         System.currentTimeMillis(), sequence.incrementAndGet() % 10000 ) ;
        //String fileName = String.format( "%s%04d", "jxnx_test",
        //        sequence.incrementAndGet() % 10000 ) ;
        return fileName ;
    }

    private String selectSubDir() {
        return String.format( "/%s",
                dirNames.get( dirSelector++ % dirNames.size() ) ) ;
    }

    private Date getCreateDate(){
        Date createDate = new Date() ;
        /* if ( fileSize == size100k ) { */
        Calendar calendar = Calendar.getInstance() ;
        calendar.setTime( createDate ) ;
        calendar.set( Calendar.MONTH, calendar.get( Calendar.MONTH ) + 1 ) ;
        createDate = calendar.getTime() ;
        return createDate ;
    }
    public void upLoad( int fileSize, boolean isUseBreakPointFile )
            throws ScmException {
        ScmFile file = ScmFactory.File.createInstance( ws ) ;
        file.setAuthor( this.session.getSessionId() ) ;
        String fileName = selectFileName() ;
        file.setFileName( fileName ) ;
        if ( fileSize <= 1024 * 1024 ) {
            if ( rand.nextBoolean() ) {
                file.setMimeType( MimeType.DOC ) ;
            } else {
                file.setMimeType( MimeType.JPEG ) ;
            }
        } else {
            int selector = rand.nextInt( 3 ) ;
            if ( selector == 0 ) {
                file.setMimeType( MimeType.AVI ) ;
            } else if ( selector == 1 ) {
                file.setMimeType( MimeType.MPEG ) ;
            } else {
                file.setMimeType( MimeType.MP3 ) ;
            }
        }

        /*Date createTime = new Date() ;
        /* if ( fileSize == size100k ) { 
        Calendar calendar = Calendar.getInstance() ;
        calendar.setTime( createTime ) ;
        calendar.set( Calendar.MONTH, calendar.get( Calendar.MONTH ) + 1 ) ;
        createTime = calendar.getTime() ;*/
        // }
        
        Date createDate = getCreateDate() ;
        file.setCreateTime( getCreateDate() ) ;
        ScmDirectory dir = curDir ;
        if ( curDir == null || count++ == fileNumPerDir ) {
            dir = ScmFactory.Directory.getInstance( ws, selectSubDir() ) ;
            curDir = dir ;
            count = 1 ;
        }
        file.setDirectory( dir ) ;

        InputStream in = selectFile( fileSize ) ;
        if ( isUseBreakPointFile ) {
            ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                    .createInstance( ws, fileName ) ;
            // breakpointFile.setCreateTime( createTime ) ;
            //breakpointFile.setCreateTime( createDate.getTime() ) ;
            breakpointFile.upload( in ) ;
            file.setContent( breakpointFile ) ;
        } else {
            file.setContent( in ) ;
        }

        file.save() ;
    }

    public void downLoad() throws ScmException {
        if ( scmIdSet.isEmpty() ) {
            load( null, 204800, false ) ;
        }

        ScmId fileId = scmIdSet.get( rand.nextInt( scmIdSet.size() ) ) ;
        ScmFile file = ScmFactory.File.getInstance( ws, fileId ) ;
        ByteArrayOutputStream os = new ByteArrayOutputStream() ;
        file.getContent( os ) ;
    }

    private int getFileSize() {
        final int[] fileSizeArr = { 204800, 1024 * 1024, 10240 * 1024,
                102400 * 1024 } ;
        return fileSizeArr[rand.nextInt( fileSizeArr.length )] ;
    }

    private String getFileType() {
        String[] fileTypeArr = { "application/msword", "image/jpeg", "video/x-msvideo",
                "audio/mpeg", "video/mpeg" } ;
        return fileTypeArr[rand.nextInt( fileTypeArr.length )] ;
    }
    
    private String getFileName(){
        return String.format("%s%04d","jxnx_test", rand.nextInt(10000)) ;
    }
    
    public void getFileObjs( int attrNum, boolean useMulType )
            throws ScmException {
     
        ScopeType moveType = ScopeType.SCOPE_CURRENT ;
        BasicBSONObject cond = new BasicBSONObject() ;
        if ( attrNum >= 1 ) {
            cond.put( "name", getFileName() ) ;
        }
        if ( attrNum >= 2 ) {
            cond.put( "size", getFileSize() ) ;
        }

        cond.put( "create_month", "201808" ) ;

        if ( !useMulType ) {
            cond.put( "mime_type", getFileType() ) ;
        }

        ScmCursor< ScmFileBasicInfo > cursor = ScmFactory.File.listInstance(
                ws, moveType, cond ) ;
        while ( cursor.hasNext() ) {
            ScmFileBasicInfo fileInfo = cursor.getNext() ;
            fileInfo.getFileId() ;
            fileInfo.getFileName() ;
        }
        cursor.close() ;
    }

    public void getBatch() throws ScmException {
        if ( batchIdSet.isEmpty() ) {
            load( null, 0, true ) ;
        }
        ScmId batchId = batchIdSet.get( rand.nextInt( batchIdSet.size() ) ) ;
        ScmBatch batch = ScmFactory.Batch.getInstance( ws, batchId ) ;
        List< ScmFile > files = batch.listFiles() ;
        for ( ScmFile file : files ) {
            ByteArrayOutputStream os = new ByteArrayOutputStream() ;
            file.getContentFromLocalSite( os ) ;
        }
    }

    private String getBatchName() {
        return String.format( "%s%d", this.session.getSessionId(),
                System.currentTimeMillis() ) ;
    }

    private ScmBreakpointFile buildBreakpointFile( InputStream in )
            throws ScmException {
        ScmBreakpointFile breakpointFile = ScmFactory.BreakpointFile
                .createInstance( ws, selectFileName() ) ;
        Date today = new Date() ;
        //breakpointFile.setCreateTime( today.getTime() ) ;
        breakpointFile.upload( in ) ;
        return breakpointFile ;

    }

    public void uploadUseBatch( int fileNum, String[] weigths )
            throws ScmException {
        if ( weigths.length != 4 )
            return ;
        ScmBatch batch = ScmFactory.Batch.createInstance( ws ) ;
        batch.setName( getBatchName() ) ;
        batch.save() ;
        for ( int i = 0; i < fileNum; ++i ) {
            ScmFile file = ScmFactory.File.createInstance( ws ) ;
            file.setAuthor( this.session.getUser() ) ;
            ScmDirectory dir = curDir ;
            if ( curDir == null || count++ == fileNumPerDir ) {
                dir = ScmFactory.Directory.getInstance( ws, selectSubDir() ) ;
                curDir = dir ;
                count = 1 ;
            }

            file.setDirectory( dir ) ;
            file.setFileName( selectFileName() ) ;
            if ( i < Integer.parseInt( weigths[0] ) ) {
                file.setContent( selectFile( size200k ) ) ;
            } else if ( i < Integer.parseInt( weigths[0] )
                    + Integer.parseInt( weigths[1] ) ) {
                file.setContent( selectFile( size1m ) ) ;
            } else if ( i < Integer.parseInt( weigths[0] )
                    + Integer.parseInt( weigths[1] )
                    + Integer.parseInt( weigths[2] ) ) {
                InputStream in = selectFile( size10m ) ;
                ScmBreakpointFile breakpointFile = buildBreakpointFile( in ) ;
                file.updateContent( breakpointFile ) ;
            } else {
                InputStream in = selectFile( size100m ) ;
                ScmBreakpointFile breakpointFile = buildBreakpointFile( in ) ;
                file.updateContent( breakpointFile ) ;
            }

            file.setCreateTime( getCreateDate() ) ;
            file.save() ;
            batch.attachFile( file.getFileId() ) ;
        }
    }

    public void fini() {
        if ( this.session != null ) {
            this.session.close() ;
        }
    }

    public static void main( String[] args ) {
        final ScmOperator oper = new ScmOperator() ;
        final Properties prop = new Properties() ;
        prop.setProperty( Common.GATEWAYURL, "192.168.30.156:8080/rootsite" ) ;
        prop.setProperty( Common.USR, "admin" ) ;
        prop.setProperty( Common.PWD, "admin" ) ;
        prop.setProperty( Common.WSNAME, "ws_jx" ) ;
        prop.setProperty( Common.DSURL, "192.168.30.156:21810" ) ;
        prop.setProperty( Common.DSUSR, "sdbadmin" ) ;
        prop.setProperty( Common.DSPWD, "sdbadmin" ) ;
        // prop.setProperty( Common.FILESIZE, Integer.toString( -1 ) ) ;
        // prop.setProperty( Common.ISNEEDLOADBATCH, "true" ) ;
        try {
            oper.init( prop ) ;

            /*for ( int i = 0; i < 10000; ++i ) {
                oper.upLoad( 204800, false ) ;
            }

            for ( int i = 0; i < 10000; ++i ) {
                oper.upLoad( 1024 * 1024, false ) ;
            }

            for ( int i = 0; i < 100; ++i ) {
                oper.upLoad( 10 * 1024 * 1024, true ) ;
            }

            for ( int i = 0; i < 10; ++i ) {
                oper.upLoad( 100 * 1024 * 1024, true ) ;
            }*/

            oper.downLoad() ;
            oper.getFileObjs( 3, false ) ;
            oper.getFileObjs( 1, true ) ;
            String[] weight = { "5", "3", "1", "1" } ;
            oper.uploadUseBatch( 10, weight ) ;
            oper.getBatch() ;

        } catch ( ScmException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace() ;
        }
    }
}
