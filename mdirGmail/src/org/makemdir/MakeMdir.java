/*
 *	Created on Nov 10, 2004
 *	Created by rmanocha
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.makemdir;


import java.io.IOException;
import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Date;
import java.util.logging.Logger;
import java.util.Properties;

import siuying.gm.GMConnector;
import siuying.gm.GMResponse;
import siuying.gm.GMConversation;
import siuying.gm.structure.GMThread;
import siuying.gm.GMConstants;
import siuying.gm.structure.GMConversationEntry;
import siuying.gm.ParsePacketException;

import org.util.*;



/**
 * @author rmanocha
 *
 */
public class MakeMdir {
    
	private transient String user;
	private transient String pwd;
	private transient GMConnector myGmailContact;
	private transient boolean unreadOnly;
	private transient String myRootdir;
	private transient String[] myLabel;
	private static Logger myLogger = Logger.getLogger("org.mdirGmail.MakeMdir");
	private transient int totalNew;
	private transient Properties myWritten;
	private transient String myWrittenFile;
	private transient String myConfFile;
	private transient final String saperator = File.separator;
	private boolean useMysql = false;
	private String sqlUser = "";
	private String sqlPass = "";
	private String sqlHost = "";
	private String sqlDb = "";
	private Connection mySqlCon;
	private Statement mySqlStmt;

	public MakeMdir(String[] args) {
		try {
			AnalyseOpts myOpts = new AnalyseOpts(args);
			if(myOpts.gotHelp()) {
				System.out.println(this.mySyntax());
				System.exit(0);
			}
			this.myWrittenFile = myOpts.getWrittenFile();
			if(myOpts.readCommandLine()) {
				this.myLabel = new String[1];
				this.user = myOpts.getUser();
				this.pwd = myOpts.getPassword();
				this.myRootdir = myOpts.getRootDir();
				this.myLabel = myOpts.getLabel();
				this.unreadOnly = myOpts.getUnreadOnly();
			}
			else {
				this.myConfFile = myOpts.getConfFile();
				this.readOptsFromFile();
			}
			if(this.myConfFile != null)
				System.out.println("Using configuration file: " + this.myConfFile);
			System.out.println("Using written file: " + this.myWrittenFile);
		} catch(ReadOptsException ex) {
			System.err.println("There was an error in the configuration file you passed.");
			System.err.println("The error message was: " + ex.getMessage());
			System.err.println(this.mySyntax());
			System.exit(2);
		} catch(AnalyseOptsException ex) {
			System.err.println("There was an error in the command line options you passed.");
			System.err.println("The error message was: " + ex.getMessage());
			System.err.println(this.mySyntax());
			System.exit(2);
		}
		/*if(!this.checkforMdir()) {
			System.err.println("The directory you provided: " + this.myRootdir + " is not a maildir directory. make sure it has three subdirectories - new, cur and tmp.");
			System.exit(1);
		}*/
		try {
			this.myWritten = new Properties();
			this.myWritten.load(new FileInputStream(this.myWrittenFile));
		} catch(IOException e) {
			System.out.println("Did not find file for reading in all written threads. Will download each message from each thread.");
			MakeMdir.myLogger.info("Did not find file for reading in all written threads. Will download each message from each thread.");
		}
		this.myGmailContact = new GMConnector(this.user,this.pwd,0);
	}
	
	private void readOptsFromFile() throws ReadOptsException {
		ReadOpts myOpts = new ReadOpts(this.myConfFile);
		this.user = myOpts.getUsername();
		this.pwd = myOpts.getPassword();
		this.myRootdir = myOpts.getBaseDir();
		this.myLabel = myOpts.getLabel();
		this.unreadOnly = myOpts.getUnread();
		if(myOpts.getUseMySql()) {
			this.useMysql = true;
			this.sqlUser = myOpts.getMySqlUser();
			this.sqlPass = myOpts.getMySqlPass();
			this.sqlHost = myOpts.getMySqlHost();
			this.sqlDb = myOpts.getMySqlDatabase();
			this.initMysql();
		}
	}
	
	private void initMysql() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			System.out.println("You asked to use MySQL to store information about downloaded messages. However, the JDBC drivers could not be found. Exiting!!");
			System.exit(1);
		}
		try {
			String jdbcconn = "jdbc:mysql://" + this.sqlHost + "/" + this.sqlDb;
			this.mySqlCon = DriverManager.getConnection(jdbcconn,this.sqlUser,this.sqlPass);
			this.mySqlStmt = this.mySqlCon.createStatement();
		} catch (SQLException e) {
			System.out.println("There was an error in connecting to the database. Please make sure all the passed values are correct. Exiting!!");
			e.printStackTrace();
			System.exit(1);
		}
	}
    
	private boolean checkforMdir() {
		if(((new File(this.myRootdir)).exists() == false) || ((new File(this.myRootdir + "new" + this.saperator)).exists() == false) || ((new File(this.myRootdir + "cur" + this.saperator)).exists() == false) || ((new File(this.myRootdir + "tmp" + this.saperator)).exists() == false))
			return false;
		return true;
	}
    
	private void checkAndCreateLabelFolder(String label) {
		File myDir = new File(this.myRootdir + label);
		if(myDir.exists() && myDir.isDirectory())
			return;
		else if(myDir.exists()) {
			System.err.println("There is a file with the same name as the label you specified in the root maildir directory. This is not allowed. Please remove this file before continuing.");
			System.exit(1);
		}
		else {
			System.out.println("Creating required directories");
			MakeMdir.myLogger.info("Creating required directories");
			if(((new File(this.myRootdir + label + this.saperator + "new")).mkdirs() == false) || ((new File(this.myRootdir + label + this.saperator + "cur")).mkdirs() == false) || ((new File(this.myRootdir + label + this.saperator + "tmp")).mkdirs() == false)) {
				System.err.println("There was an error in creating the rquired directories.");
				System.exit(1);
			}
		}
	}
    public void execute() {
		System.out.println("Logging in...");
		if(this.myGmailContact.connect() == false) {
			System.err.println("Error in connection. This could be due to the fact that you are trying to log in too often.");
			System.exit(2);
		}
		System.out.println("Logged in.");
		for(int j = 0; j < this.myLabel.length;j++) {
			System.out.println("Fetching label: " + this.myLabel[j]);
			MakeMdir.myLogger.info("Fetching label: " + this.myLabel[j]);
			if(!this.myLabel[j].equalsIgnoreCase("inbox"))
				this.checkAndCreateLabelFolder(this.myLabel[j]);
			try {
				GMResponse myResponse = this.myGmailContact.request(siuying.gm.GMConstants.GM_REQ_LABEL, this.myLabel[j], "");
				TreeMap tree = myResponse.getGminfo();
				int numThreads = Integer.parseInt((String)tree.get("current.box.totalthreads"));
				System.out.println("There are a total of " + numThreads + " in this folder.");
				int numThreadspp = Integer.parseInt((String)tree.get("current.box.threadsperpage"));
				int numPages = numThreads / numThreadspp;
				if((numThreads % numThreadspp) != 0)
					numPages ++;
				System.out.println("There are " + numPages + " pages and " + numThreadspp + " threads per page");
				for(int i = 0; i < numPages; i++) {
					myResponse = this.myGmailContact.request(siuying.gm.GMConstants.GM_REQ_LABEL,this.myLabel[j],Integer.toString(i));
					ArrayList myThreads = myResponse.getGMThreads();
					if(this.unreadOnly) {
						if(this.populateUnreadOnly(myThreads,this.myLabel[j]))
							break;
					}
					else
						this.populateAll(myThreads,this.myLabel[j]);
				}
			} catch(IOException e) {
				System.err.println("There was an error in the request sent");
				e.printStackTrace();
			}catch(ParsePacketException e) {
				System.err.println("Gmail sent something unexpected.Sorry");
				e.printStackTrace();
			}
		}
		try {
			this.myWritten.store(new FileOutputStream(this.myWrittenFile),"");
		} catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println(this.totalNew + " new messsages were downloaded.");
	}

    
	private boolean populateUnreadOnly(ArrayList myThreads, String label) throws IOException, ParsePacketException {
		for(int i = 0; i < myThreads.size(); i++) {
			GMThread tmp = (GMThread)myThreads.get(i);
			if(!tmp.isRead()) {
				GMConversation conv = (GMConversation)this.myGmailContact.request(GMConstants.GM_REQ_CONVERSATION,tmp.getThreadID(),"0");
				ArrayList convEntries = conv.getEntries();
				for(int j = this.getProperties(((GMConversationEntry)convEntries.get(0)).getId(),label); j < convEntries.size() ; j++) {
					Date date = new Date();
					GMConversationEntry entry = (GMConversationEntry)convEntries.get(j);
					String uID = entry.getId();
					String message = this.myGmailContact.fetchOriginalMail(uID).trim();
					System.out.println("Writing message: " + uID);
					this.putTmp(Long.toString(date.getTime()) + "." + uID,message,label,"new");
					this.totalNew++;
				}
				this.setProperties(((GMConversationEntry)convEntries.get(0)).getId(),Integer.toString(convEntries.size()));
			}
			else
				return true;
		}
		return false;
	}
    
	private void populateAll(ArrayList myThreads, String label) throws IOException, ParsePacketException {
		for(int i = 0; i < myThreads.size(); i++) {
			GMThread tmp = (GMThread)myThreads.get(i);
			GMConversation conv = (GMConversation)this.myGmailContact.request(GMConstants.GM_REQ_CONVERSATION,tmp.getThreadID(),"0");
			ArrayList convEntries = conv.getEntries();
			for(int j = this.getProperties(((GMConversationEntry)convEntries.get(0)).getId(),label); j < convEntries.size() ; j++) {
				Date date = new Date();
				GMConversationEntry entry = (GMConversationEntry)convEntries.get(j);
				String uID = entry.getId();
				String message = this.myGmailContact.fetchOriginalMail(uID).trim();
				System.out.println("Writing message: " + uID);
				if(((GMThread)myThreads.get(i)).isRead()) {
					this.putTmp(Long.toString(date.getTime()) + "." + uID + ":2,S",message,label,"cur");
				}
				else {
					this.putTmp(Long.toString(date.getTime()) + "." + uID,message,label,"new");
					this.totalNew++;
				}
			}
			this.setProperties(((GMConversationEntry)convEntries.get(0)).getId(),Integer.toString(convEntries.size()));
		}
	}
    
	private boolean putTmp(String name, String data, String label, String toDir) throws IOException {
		String fullname = "";
		String movename = "";
		if(label.equalsIgnoreCase("inbox")) {
			fullname = this.myRootdir + "tmp" + this.saperator + name;
			movename = this.myRootdir + toDir + this.saperator + name;
		}
		else {
			fullname = this.myRootdir + this.saperator + label + this.saperator + "tmp" + this.saperator + name;
			movename = this.myRootdir + this.saperator + label + this.saperator + toDir + this.saperator + name;
		}
		PrintStream myOut = new PrintStream(new FileOutputStream(fullname));
		myOut.print(data.replaceAll("\r",""));
		myOut.close();
		File tmpFile = new File(fullname);
		tmpFile.renameTo(new File(movename));
		MakeMdir.myLogger.info("Written file: " + movename);
		return true;
	}
	
	/**
	 * This function will know where to get the required information from(from a mysql database or a plain file.
	 */
	private int getProperties(String id,String label) {
		if(!this.useMysql) {
			String tmp = this.myWritten.getProperty(id,"0");
			if(tmp.equals("0"))
				MakeMdir.myLogger.info(id + " not found");
			else
				MakeMdir.myLogger.info(tmp + " messages found for " + id);
			return Integer.parseInt(tmp);
		} else {
			try {
				ResultSet rs = this.mySqlStmt.executeQuery("SELECT length FROM " + label + "WHERE `id` = \"" + id + "\"");
				return rs.getInt("length");
			} catch (SQLException e) {
				System.out.println("Some error in database access. Exiting!!");
				e.printStackTrace();
				System.exit(1);
			}
		}
		return 0;
	}
	
	/**
	 * This currently only saves into the written file. However, this method will know whether to put the 
	 * required stuff into a mysql database or a file(once i implement that stuff....).
	 * @param id
	 * @param size
	 */
	private void setProperties(String id, String size) {
		this.myWritten.setProperty(id,size);
	}

	private String mySyntax() {
		String syntax = "MdirGavaMail v0.2";
		syntax += "\nUsage 0: mdirgavamail --help";
		syntax += "\nUsage 1: mdirgavamail <username> <password> <base-maildir> <label> [--unread-ony] [--written <filename>]";
		syntax += "\nUsage 2: mdirgavamail [--conf <filename>] [--written <filename>]";
		syntax += "\nusername\tYour gmail username";
		syntax += "\npassword\tYour gmail password";
		syntax += "\nbase-maildir\tTop level maildir directory.This should end with a \"/\" (or \"\\\" for windows)";
		syntax += "\nlabel\t\tThe label you want to download. Inbox and ALL are also considered labels.";
		syntax += "\n--unread-only\tIf you want to download unread messages only.";
		syntax += "\n--written\tThe file where to store info about the messages we have downloaded already.(defaults to .mdirgmailwritten)";
		syntax += "\n--conf\t\tConfiguration file to be used to get various required data.(defaults to ./.mdirgmailrc)";
		syntax += "\n--help\t\tPrints out this message and exits. This should only be passed individually.";
		return syntax;
	}

	public static void main(String[] args) {
		MakeMdir myMdr = new MakeMdir(args);
		MakeMdir.myLogger.info("Finished initialising.");
		myMdr.execute();
	}
}