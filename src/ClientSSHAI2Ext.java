package ext.appinventor.SSH.ClientSSHAI2Ext;
/**
 * Simple Client SSH based on the JSCH library
 * @author aluis.rcastro@bol.com.br
 * @Date 2019.05.08
 * Copyright (c) 2019 andre luis ramos de castro
 */
 
//   Copyright (c) 2002-2018 ymnk, JCraft,Inc. All rights reserved.
//   Copyright 2009-2011 Google, All Rights reserved
//   Copyright 2011-2012 MIT, All rights reserved

//   This code is provided "as-is", which means no implicit or explicit warranty

import com.google.appinventor.components.runtime.*;
	 
import android.os.Environment;
import android.os.AsyncTask;
	 
import javax.swing.JOptionPane;
	 
import com.google.appinventor.components.runtime.util.RuntimeErrorAlert;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.util.AsynchUtil;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.SdkLevel;
	 
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.os.StrictMode;

import java.io.ByteArrayOutputStream; 
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.SocketException;
	
import java.util.concurrent.*;

import com.jcraft.jsch.*;
import com.jcraft.jzlib.*;

import javax.swing.*;
import java.io.*;

/**
 * General config parameters
 */
@DesignerComponent(version = 2,
		description = "Non-visible component that provides client ssh connectivity.",
		category = ComponentCategory.EXTENSION,
		nonVisible = true,
		iconName = "https://www.edaboard.com/attachment.php?attachmentid=152944&d=1557344512")
@SimpleObject(external = true)
@UsesLibraries(libraries = "jsch-0.1.54.jar")
@UsesPermissions(permissionNames =  	"android.permission.INTERNET," +
				    	"android.permission.CHANGE_NETWORK_STATE," +
				    	"android.permission.ACCESS_WIFI_STATE," +
   				    	"android.permission.ACCESS_NETWORK_STATE," +
					"android.permission.WRITE_EXTERNAL_STORAGE," +
					"android.permission.READ_EXTERNAL_STORAGE," +									
					"android.permission.WRITE_SETTINGS," +
					"android.permission.WRITE_SYNC_SETTINGS," +
					"android.permission.PERSISTENT_ACTIVITY," +
					"android.permission.CHANGE_CONFIGURATION," +
					"android.permission.READ_PHONE_STATE")								

public class ClientSSHAI2Ext extends AndroidNonvisibleComponent implements Component
{
    	private static final String LOG_TAG = "ClientSSHAI2Ext";
	
	private String host 	= "TO_BE_FILLED_BY_APP";	// Don't need to be in format "USER@HOST"
	private String user 	= "TO_BE_FILLED_BY_APP";
	private String passwd 	= "TO_BE_FILLED_BY_APP";
	private int    port		= 22;
	
	// boolean that indicates the state of the connection, true = connected, false = not connected
    	private boolean ConnectionState = false;
	// Debug Purpose only
	private String DebugText = "";
	// String received message from remote SSH terminal
    	private String ReceivedMessage	= "";
	// boolean that indicates whether button "start connection" whas pressed or not
    	private boolean bButtonStart = false;	
	// String containing the SSH command to be issued to the remote terminal
	private	String command = "";
	// boolean that enables raising events due to new debug messages
	private boolean EventFromDebugMessages = false;
	

    	private final Activity activity;

    	InputStream inputStream = null;
	
					
    /**
     * Creates a new Client SSH component.
     *
     * @param container the Form that this component is contained in.
     */
    public ClientSSHAI2Ext(ComponentContainer container)
    {
        super(container.$form());
        activity = container.$context();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);		
    }

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that enable raising event messages (may overload core processing with useless information, most the time) 
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Method that enable raising event messages")
    public void EnableEventFromDebugMessages(boolean state)
    {
        EventFromDebugMessages = state;
    }	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that set the single-line Bash/Shell Command to be issued to the remote server
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "String containing the Bash or Shell Command to be issued to the remote SSH server")
    public void SetCommand(String sCommand)
    {
        command = sCommand;
    }	
	
    /**
     * Method that retrieve Command variable content
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Retrieve Command variable content")
    public String GetCommand()
    {
        return command;
    }
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns the connection state
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get state of the connection - true = connected, false = disconnected")
    public boolean GetConnectionState()
    {
        return ConnectionState;
    }
	
    /**
     * Method that set the connection state
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set state of the connection - true = connected, false = disconnected")
    public void SetConnectionState(boolean state)
    {
        ConnectionState = state;
    }	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns the Host
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get HOST")
    public String GetHost()
    {
        return host;
    }
	
    /**
     * Method that set the Host
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set HOST - do not fill with USER@HOST format !")
    public void SetHost(String sHost)
    {
        host = sHost;
    }	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns the User
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get USER")
    public String GetUser()
    {
        return user;
    }
	
    /**
     * Method that set the User
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set USER")
    public void SetUser(String sUser)
    {
        user = sUser;
    }	
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns the Password
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get PASSWORD")
    public String GetPassword()
    {
        return passwd;
    }
	
    /**
     * Method that set the Password
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set PASSWORD")
    public void SetPassword(String sPasswd)
    {
        passwd = sPasswd;
    }	

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns Debug message Text
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get Debug Text")
    public String GetDebugText()
    {
        return DebugText;
    }
    
    /**
     * Method that set Debug message Text
     */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Set Debug Text")	
    public void SetDebugText(String Text)
    {
        DebugText = Text;
		if ( EventFromDebugMessages == true)
			NewDebugMessage(Text);
    }

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Method that returns the SSH Text from remote server
     */
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "read text from remote SSH server")
    public String GetReceivedMessage()
    {
        return ReceivedMessage;
    }
	
    public void SetReceivedMessage(String Text)
    {
        ReceivedMessage = Text;
    }

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Event indicating that there is available new text line
     *
     */
	@SimpleEvent
    public void NewDebugMessage(String msgDbg)
    {
        // invoke the application's "NewDebugMessage" event handler.
        EventDispatcher.dispatchEvent(this, "NewDebugMessage", msgDbg);
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Event indicating that there is available new text line
     *
     */
	@SimpleEvent
    public void NewIncomingMessage(String msgIn)
    {
        // invoke the application's "NewIncomingMessage" event handler.
        EventDispatcher.dispatchEvent(this, "NewIncomingMessage", msgIn);
    }
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
   /**
     * Send cmd through ssh to the server
     */
    @SimpleFunction(description = "Send cmd to the server")
    public void SendData( final String command ) {
		bButtonStart = true;
		SetDebugText("Start SendData function");
        if ( GetConnectionState() == true ) {
			SetDebugText("Already connected");
			bButtonStart = false;
		}
		else{
			try {	
				final JSch jsch = new JSch();							
				SetDebugText("Attempt to connect");	
				AsynchUtil.runAsynchronously(new Runnable()
				{
					@Override
					public void run() 
					{
					SetDebugText("Run runAsynchronously");
						if ( GetConnectionState() == false )
						{
							if ( bButtonStart == true )
								{	
								try{
									SetDebugText("Try within runAsynchronously");  		
									java.util.Properties config = new java.util.Properties(); 
									config.put("StrictHostKeyChecking", "no");
									JSch jsch = new JSch();
									SetDebugText("JSch instantiated");
									Session session=jsch.getSession(user, host, 22);
									session.setTimeout(10000);
									session.setPassword(passwd);  
									session.setConfig(config);
									session.connect(30000); 		// making a connection with expressive timeout.
									SetDebugText("Session connected");
									SetConnectionState(true);
									Channel channel=session.openChannel("exec");
									((ChannelExec)channel).setCommand(command);
									channel.setInputStream(System.in);
									channel.setOutputStream(System.out);
									((ChannelExec)channel).setErrStream(System.err);
									InputStream in=channel.getInputStream();
									channel.connect();
									SetDebugText("Channel connected");
									byte[] tmp=new byte[1024];
									String RecBytes	= "";
									SetDebugText("Started receiving");
									while(true){
										while(in.available()>0){
										  int i=in.read(tmp, 0, 1024);
										  if(i<0)
											  break;
										  RecBytes = new String(tmp, 0, i) ;
										  SetReceivedMessage(RecBytes);
										  SetDebugText("receiving...");
										}
										activity.runOnUiThread(new Runnable()
										  {
											@Override
											public void run()
											{
											  NewIncomingMessage(GetReceivedMessage());
											}
										  } );
										SetDebugText("Got text");
										// NewIncomingMessage(GetReceivedMessage());
										if(channel.isClosed()){
											  if(in.available()>0) 
												  continue; 
										  System.out.println("exit-status: "+ channel.getExitStatus());
										  break;
										}try
											{
											Thread.sleep(1000);
											SetDebugText("Sleeping");
											}
											catch(Exception errSleep){
												SetDebugText ("error attempting to sleep");
												break;
											}
										if ( GetConnectionState() == false )
											break;
									  }

									SetDebugText("Exit from infinite loop - receive");
									channel.disconnect();
									SetDebugText("Channel disconnected");
									session.disconnect();		
									SetDebugText("Session disconnected");
									SetConnectionState(false);
									SetDebugText("End of reception");
								}
								catch ( Exception  exception  ) {
									SetDebugText ("Exception: " + exception.getMessage());
								}
							}
						}							
					}	
				});
				}
				catch(Exception err){
					SetConnectionState(false);
					bButtonStart = false;
					// System.out.println("Got runtime error: " + e);
					// throw new YailRuntimeError("err7", "Error");
					// SetDebugText("err");
				}		
		}
	}	
}
