/**
 * Copyright (c) 2018, SequoiaDB Ltd.
 * File Name:TestBatchDownLoad.java
 * 类的详细描述
 *
 *  @author 类创建者姓名
 * Date:2018-7-30下午12:36:43
 *  @version 1.00
 */
package com.scm.perftest;

import java.util.Properties ;

import org.apache.jmeter.config.Arguments ;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient ;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext ;
import org.apache.jmeter.samplers.SampleResult ;

import com.sequoiacm.client.exception.ScmException ;


public class TestBatchDownLoad extends AbstractJavaSamplerClient{
    private ScmOperator oper = null ;

    @Override
    public Arguments getDefaultParameters() {
        Arguments args = new Arguments() ;
        args.addArgument( Common.CASENAME, "batchdownload" ) ;
        args.addArgument( Common.GATEWAYURL, "192.168.30.156:8080/rootsite" ) ;
        args.addArgument( Common.USR, "admin" ) ;
        args.addArgument( Common.PWD, "admin" ) ;
        args.addArgument( Common.DSURL, "192.168.30.156:21810" ) ;
        args.addArgument( Common.DSUSR, "sdbadmin" ) ;
        args.addArgument( Common.DSPWD, "sdbadmin" ) ;
        args.addArgument( Common.WSNAME, "ws_jx" ) ;

        return args ;

    }

    @Override
    public void setupTest( JavaSamplerContext args ) {  
        Properties prop = new Properties();
        prop.setProperty( Common.GATEWAYURL, args.getParameter( Common.GATEWAYURL ) ) ;
        prop.setProperty( Common.USR, args.getParameter( Common.USR ) ) ;
        prop.setProperty( Common.PWD, args.getParameter( Common.PWD ) ) ;
        prop.setProperty( Common.WSNAME, args.getParameter( Common.WSNAME )  ) ;
        prop.setProperty( Common.DSURL, args.getParameter( Common.DSURL ) ) ;
        prop.setProperty( Common.DSUSR, args.getParameter( Common.DSUSR ) ) ;
        prop.setProperty( Common.DSPWD, args.getParameter( Common.DSPWD ) ) ;
        prop.setProperty( Common.ISNEEDLOADBATCH, Boolean.toString( true) ) ;
        try {
            oper = new ScmOperator( ) ;
            oper.init( prop ) ;
        } catch ( ScmException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace() ;
        }
    }

    @Override
    public SampleResult runTest( JavaSamplerContext arg0 ) {
        // TODO Auto-generated method stub
        SampleResult rs = new SampleResult() ;
        rs.setSampleLabel( arg0.getParameter( Common.CASENAME )) ;
        rs.sampleStart() ;
        rs.setDataType( SampleResult.TEXT ) ;
        try{
            oper.getBatch() ;
            rs.setSuccessful( true ) ;
        }catch( ScmException e ){
            rs.setSuccessful( false ) ;
            e.printStackTrace() ;
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
