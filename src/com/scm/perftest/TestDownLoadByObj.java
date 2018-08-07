/**
 * Copyright (c) 2018, SequoiaDB Ltd.
 * File Name:TestDownLoadByObj.java
 * 类的详细描述
 *
 *  @author 类创建者姓名
 * Date:2018-7-30下午12:22:35
 *  @version 1.00
 */
package com.scm.perftest;

import java.util.Properties ;

import org.apache.jmeter.config.Arguments ;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient ;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext ;
import org.apache.jmeter.samplers.SampleResult ;
import com.sequoiadb.exception.BaseException ;


public class TestDownLoadByObj extends AbstractJavaSamplerClient{
    private SdbOperator oper = null ;
    
    @Override
    public Arguments getDefaultParameters() {
        Arguments args = new Arguments() ;
        args.addArgument( Common.CASENAME, "downloadbyobj" ) ;
        args.addArgument( Common.DBURL, "192.168.30.156:21810" ) ;
        args.addArgument( Common.DBUSER, "sdbadmin" ) ;
        args.addArgument( Common.DBPWD, "sdbadmin" ) ;
        args.addArgument( Common.COLLECTIONSPACENAME, "test" ) ;
        args.addArgument( Common.COLLECTIONNAME, "test" ) ;
        
        return args ;
    }

    @Override
    public void setupTest( JavaSamplerContext args ) { 
        Properties prop = new Properties();
        prop.setProperty( Common.DBURL, args.getParameter( Common.DBURL ) ) ;
        prop.setProperty( Common.DBUSER, args.getParameter( Common.DBUSER ) ) ;
        prop.setProperty( Common.DBPWD, args.getParameter( Common.DBPWD ) ) ;
        prop.setProperty( Common.COLLECTIONSPACENAME, args.getParameter( Common.COLLECTIONSPACENAME ) ) ;
        prop.setProperty( Common.COLLECTIONNAME, args.getParameter( Common.COLLECTIONNAME ) ) ;
      
        try {
            oper = new SdbOperator( ) ;
            oper.init( prop ) ;
        } catch ( BaseException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace() ;
        }
    }

    @Override
    public SampleResult runTest( JavaSamplerContext arg0 ) {
        // TODO Auto-generated method stub
        SampleResult rs = new SampleResult() ;
        rs.setSampleLabel( arg0.getParameter( Common.CASENAME ) ) ;
        rs.sampleStart() ;
        rs.setDataType( SampleResult.TEXT ) ;
        try{
            oper.downLoad() ;
            rs.setSuccessful( true ) ;
        }catch( BaseException e){
            e.printStackTrace() ;
            rs.setSuccessful( false ) ;
        }
        rs.sampleEnd() ;
        return rs ;
    }

    @Override
    public void teardownTest( JavaSamplerContext arg0 ) {
        if ( null != oper ) {
            oper.fini() ;
        }
    }
}
