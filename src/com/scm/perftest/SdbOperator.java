/**
 * Copyright (c) 2018, SequoiaDB Ltd.
 * File Name:SdbOperator.java
 * 类的详细描述
 *
 *  @author 类创建者姓名
 * Date:2018-7-30上午9:57:46
 *  @version 1.00
 */
package com.scm.perftest;

import java.io.ByteArrayInputStream ;
import java.io.ByteArrayOutputStream ;
import java.io.InputStream ;
import java.util.Properties ;
import java.util.Random ;
import java.util.concurrent.atomic.AtomicInteger ;

import org.bson.BasicBSONObject ;
import org.bson.types.ObjectId ;

import com.sequoiadb.base.CollectionSpace ;
import com.sequoiadb.base.DBCollection ;
import com.sequoiadb.base.DBCursor ;
import com.sequoiadb.base.DBLob ;
import com.sequoiadb.base.DBQuery ;
import com.sequoiadb.base.Sequoiadb ;


public class SdbOperator {
    private Sequoiadb db = null ;
    private DBCollection cl ;
    private final static int size100m = 100 * 1024 * 1024 ;
    private static byte[] d100m = new byte[ size100m ] ;
    private static AtomicInteger sequence = new AtomicInteger(0) ;
    private static Random rand = new Random() ;
    private static AtomicInteger count = new AtomicInteger(0) ;
    
    public SdbOperator( ){
        
    }
    
    public void init( Properties prop ){
        String url = prop.getProperty( Common.DBURL, "192.168.30.156:22180" ) ;
        String user = prop.getProperty( Common.DBUSER, "" ) ;
        String pwd = prop.getProperty( Common.DBPWD, "" ) ;
        
        String csName = prop.getProperty( Common.COLLECTIONSPACENAME ) ;
        String clName = prop.getProperty( Common.COLLECTIONNAME ) ;
       
        db  = new Sequoiadb( url, user, pwd) ;
        CollectionSpace cs = db.getCollectionSpace( csName ) ;
        cl = cs.getCollection( clName ) ;
        
        synchronized ( count ) {
            if ( count.getAndIncrement() == 0 ) {
                boolean isExist = false ;
                BasicBSONObject cond = new BasicBSONObject() ;
                cond.put( "_id", cl.getFullName() ) ;
                DBCursor cursor = cl.query( cond, null, null, null, 0, -1,
                        DBQuery.FLG_QUERY_WITH_RETURNDATA ) ;
                while ( cursor.hasNext() ) {
                    sequence.set( ( ( BasicBSONObject ) cursor.getNext() )
                            .getInt( "sequence" ) ) ;
                    isExist = true ;
                }
                cursor.close() ;

                if ( !isExist ) {
                    BasicBSONObject doc = new BasicBSONObject() ;
                    doc.put( "_id", cl.getFullName() ) ;
                    doc.put( "sequence", 0 ) ;
                    cl.insert( doc ) ;
                }
            }
        }
    }
    
    private InputStream selectFile( int fileSize ) {
        InputStream in = null ;
        in = new ByteArrayInputStream( d100m, 0, fileSize ) ;
        return in ;
    }
    
    private String getwOid(){
        return String.format( "%024d", sequence.getAndIncrement() ) ;
    }
    
    private String getrOid(){
        return String.format( "%024d", rand.nextInt(sequence.get()) ) ;
    }
    
    public void upLoad( int fileSize ){
        ObjectId oid = new ObjectId( getwOid() ) ;
        DBLob lob = cl.createLob( oid ) ;
        lob.write( selectFile( fileSize) ) ;
        lob.close() ;
    }
    
    public void downLoad( ){
        ObjectId oid = new ObjectId( getrOid() ) ;
        DBLob lob = cl.openLob( oid );
        lob.read( new ByteArrayOutputStream() ) ;
        lob.close() ;
    }
    
    public void fini(){
        if ( count.decrementAndGet() == 0 ){
            BasicBSONObject cond = new BasicBSONObject() ;
            cond.put( "_id", cl.getFullName() );
            BasicBSONObject update = new BasicBSONObject() ;
            BasicBSONObject sub = new BasicBSONObject() ;
            sub.put( "sequence",  sequence.get() ) ;
            update.put( "$set", sub ) ;
            cl.update(cond, update, null) ;
        }
        
        if ( db != null){
            db.close() ;
        }
        
    }
    
    public static void main(String[] args){
        Properties prop = new Properties() ;
        prop.setProperty( Common.DBURL, "192.168.30.66:11810" );
        prop.setProperty( Common.DBUSER, "sdbadmin" );
        prop.setProperty( Common.DBPWD, "sdbadmin" );
        prop.setProperty( Common.COLLECTIONSPACENAME, "test");
        prop.setProperty( Common.COLLECTIONNAME, "test" );
        
        SdbOperator oper = new SdbOperator() ;
        oper.init(prop) ;
        oper.upLoad( 100 * 1024 ) ;
        oper.upLoad( 100 * 1024 ) ;
        oper.downLoad() ;
        oper.fini();
         
    }
}
