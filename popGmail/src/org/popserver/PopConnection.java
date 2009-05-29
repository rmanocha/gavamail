/*
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
 
package org.popserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.TreeMap;


import siuying.gm.GMConnector;
import siuying.gm.GMResponse;
import siuying.gm.GMConversation;
import siuying.gm.structure.GMThread;
import siuying.gm.GMConstants;
import siuying.gm.structure.GMConversationEntry;
import siuying.gm.ParsePacketException;


public class PopConnection implements Runnable {

	private static Logger logger = Logger.getLogger("org.popserver.PopConnection");
	private static Pattern userPat = Pattern.compile("\\AUSER\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static Pattern passPat = Pattern.compile("\\APASS\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static Pattern quitPat = Pattern.compile("\\AQUIT\\s*\\Z", Pattern.CASE_INSENSITIVE);
	private static Pattern statPat = Pattern.compile("\\ASTAT\\s*\\Z", Pattern.CASE_INSENSITIVE);
	private static Pattern listPat = Pattern.compile("\\ALIST\\s*\\Z", Pattern.CASE_INSENSITIVE);
	private static Pattern listNumPat = Pattern.compile("\\ALIST\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);
	private static Pattern retrPat = Pattern.compile("\\ARETR\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);
	private static Pattern topPat = Pattern.compile("\\ATOP\\s+([0-9]+)\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);
	private static Pattern delPat = Pattern.compile("\\ADELE\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);
	private static Pattern noopPat = Pattern.compile("\\ANOOP", Pattern.CASE_INSENSITIVE);
	private static Pattern rsetPat = Pattern.compile("\\ARSET", Pattern.CASE_INSENSITIVE);
	private static Pattern uidlPat = Pattern.compile("\\AUIDL\\s*\\Z", Pattern.CASE_INSENSITIVE);
	private static Pattern uidlNumPat = Pattern.compile("\\AUIDL\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);
	private static final byte RESET = 0;
	private static final byte AUTHORIZATION = 1;
	private static final byte TRANSACTION = 2;
	private static final byte UPDATE = 3;
	public static boolean unreadOnly = false;
	private byte state;
	private boolean have_username = false;
	private String login;
	private String password;
	protected BufferedReader in;
	protected PrintWriter out;
	protected Socket myClient;
	private GMConnector myGmailContact;
	private ArrayList myMails;
	    
	public PopConnection(Socket socket) {
		this.setSocket(socket);
		this.myMails = new ArrayList();
	}
	
	private void setSocket(Socket socket) {
		this.myClient = socket;
		try {
           	this.in = new BufferedReader(new InputStreamReader(this.myClient.getInputStream()));
            this.out = new PrintWriter(this.myClient.getOutputStream(),true);
       	} catch (IOException e) {
            PopConnection.logger.log(Level.SEVERE, "should not happen", e);
        }
        this.state = PopConnection.AUTHORIZATION;
	}
	
	public void resetSocket() throws IOException {
        if (this.myClient != null) {
            PopConnection.logger.log(Level.WARNING, "my client is not null");
           	this.myClient.close();
        }
        PopConnection.logger.log(Level.WARNING, "Inside resetSocket()");
        this.myClient = null;
        this.myMails = null;
        this.in = null;
        this.out = null;
        this.state = PopConnection.RESET;
	}
	
	public void run() {
		try {
           	String line;
           	boolean done = false;
           	this.sendOk("gavamail gmail proxy ready");
           	PopConnection.logger.info("Got into run");
           	while (!done) {
           		line = this.in.readLine();
           		if (line != null) {
           			PopConnection.logger.info(line + " was passed");
           			done = this.parseLine(line);
           		} else {
           			PopConnection.logger.log(Level.INFO, "client socket closed without QUIT");
           			done = true;
           		}
           	}
        } catch (Exception e) {
        	PopConnection.logger.log(Level.SEVERE, "should not happen", e);
        } finally {
        	try {
				this.resetSocket();
				PopConnection.logger.info("resetSocket() is done.");
			} catch (Exception e) {
				PopConnection.logger.log(Level.SEVERE, "should not happen", e);
			}
		}
        PopConnection.logger.info("run() is done.");
	}
	
	private void sendOk(String line) {
		this.out.print("+OK " + line + "\r\n");
		PopConnection.logger.fine("+OK " + line);
		this.out.flush();
	}
	
	private void sendErr(String line) {
		this.out.print("-ERR " + line + "\r\n");
		PopConnection.logger.fine("-ERR " + line);
		this.out.flush();
	}
	
	private boolean populateUnreadOnly(ArrayList myThreads) throws IOException, ParsePacketException{
		for(int i = 0; i < myThreads.size(); i++) {
			if(!((GMThread)myThreads.get(i)).isRead()) {
				GMConversation conv = (GMConversation)this.myGmailContact.request(GMConstants.GM_REQ_CONVERSATION,((GMThread)myThreads.get(i)).getThreadID(),"0");
				ArrayList convEntries = conv.getEntries();
            	for(int j = 0; j < convEntries.size() ; j++)
            		this.myMails.add((GMConversationEntry)convEntries.get(j));
			}
			else
				return true;
		}
		return false;
	}
	
	private void populateAll(ArrayList myThreads) throws IOException, ParsePacketException{
		for(int i = 0; i < myThreads.size(); i++) {
			GMConversation conv = (GMConversation)this.myGmailContact.request(GMConstants.GM_REQ_CONVERSATION,((GMThread)myThreads.get(i)).getThreadID(),"0");
			ArrayList convEntries = conv.getEntries();
			for(int j = 0; j < convEntries.size() ; j++) {
				this.myMails.add((GMConversationEntry)convEntries.get(j));
			}
		}
	}
    
	public boolean parseLine(String line) {
		if (PopConnection.passPat.matcher(line).find()) {
			PopConnection.logger.fine("pop3: PASS ********");
       	}else {
       		PopConnection.logger.fine("pop3: " + line);
      	}
		if (line == null) {
			this.sendOk("");
			return false;
      	}
		//login
		if (PopConnection.userPat.matcher(line).find()) {
			Matcher matcher = PopConnection.userPat.matcher(line);
			matcher.find();
			this.login = new String(matcher.group(1));
			this.sendOk("user " + matcher.group(1) + " accepted");
			this.have_username = true;
			return false;
		}
		else if (PopConnection.passPat.matcher(line).find()) {
			if (!this.have_username) {
				this.sendErr("please provide username first");
				return false;
			}
			Matcher matcher = PopConnection.passPat.matcher(line);
			matcher.find();
			this.password = new String(matcher.group(1));
			this.myGmailContact = new GMConnector(this.login,this.password,0);
			PopConnection.logger.info("Trying to login");
			if(this.myGmailContact.connect() == false) {
				this.sendErr("Authorization failed. Stupid you");
				return true;
			}
			this.sendOk("password accepted");
			this.state = PopConnection.TRANSACTION;
			PopConnection.logger.info("Logged in ok");
			try {
				GMResponse myResponse = this.myGmailContact.request(siuying.gm.GMConstants.GM_REQ_STANDARD, "ALL", "");
				TreeMap tree = myResponse.getGminfo();
				int numThreads = Integer.parseInt((String)tree.get("current.box.totalthreads"));
				int numThreadspp = Integer.parseInt((String)tree.get("current.box.threadsperpage"));
				int numPages = numThreads / numThreadspp;
				if((numThreads % numThreadspp) != 0)
					numPages ++;
				for(int i = 0; i < numPages; i++) {
					myResponse = this.myGmailContact.request(siuying.gm.GMConstants.GM_REQ_STANDARD,"ALL",Integer.toString(i));
					ArrayList myThreads = myResponse.getGMThreads();
					if(PopConnection.unreadOnly) {
						if(this.populateUnreadOnly(myThreads))
							break;
					}
					else
						this.populateAll(myThreads);
				}
			}catch(IOException e) {
				this.sendErr("There was an error in the request sent");
				return false;
			}catch(ParsePacketException e) {
				this.sendErr("Gmail sent something unexpected.Sorry");
				return false;
			}
			return false;
		}	//QUIT
		else if (PopConnection.quitPat.matcher(line).find()) {
			if (this.myGmailContact != null) {
				this.myGmailContact.disconnect();
			}
			this.myMails = null;
			this.sendOk("Good Bye");
			return true;
		}	//NOOP
		else if (PopConnection.noopPat.matcher(line).find()) {
			if (this.state == PopConnection.TRANSACTION) {
				this.sendOk("whatever");
			} else {
				this.sendErr("not in transaction state");
			}
			return false;
		}	//DELETE
		else if (PopConnection.delPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("not in transaction state");
				return false;
			}
			this.sendErr("Deleting messages is not supported.");
			return false;
		}	//LIST	this should work with multiple conversations
		else if (PopConnection.listPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("not in transaction state");
				return false;
			}
			this.sendOk("listing with no index");
			for(int i = 0; i < this.myMails.size(); i++)
				this.out.print((i + 1) + " " + ((GMConversationEntry)this.myMails.get(i)).getBody().length()+ "\r\n");
			this.out.print(".\r\n");
			this.out.flush();
			return false;
		}	//LIST NUM
		else if (PopConnection.listNumPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("not in transaction state");
				return false;
			}
			int index = 0;
			Matcher matcher = PopConnection.listNumPat.matcher(line);
			matcher.find();
			try {
				index = (new Integer(matcher.group(1))).intValue();
			} catch (Exception e) {
				this.sendErr("Not a valid index number A -- " + matcher.group(1));
				return false;
			}
			if (index == 0) {
				this.sendErr("Not a valid index number B");
				return false;
			} else if ((index > this.myMails.size()) || (index < 1)) {
				this.sendErr("Not a valid index number C");
				return false;
			} else if (this.myMails.size() == 0) {
				this.sendErr("Not a valid index number D");
				return false;
			}
			this.sendOk(index + " " + ((GMConversationEntry)this.myMails.get(index - 1)).getBody().length());
			return false;
		}	//STAT		This should work with myltiple conversationEntries
		else if (PopConnection.statPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("not in transaction state");
				return false;
			}
			if (this.myMails.size() < 1) {
				this.sendOk("0 0");
				return false;
			}
			int totalSize = 0;
			for(int i = 0; i < this.myMails.size(); i++)
				totalSize += ((GMConversationEntry)this.myMails.get(i)).getBody().length();
			this.sendOk(this.myMails.size() + " " + totalSize);
			return false;
		} 	//Retrieve(RETR)
		else if (PopConnection.retrPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("not in transaction state");
				return false;
			}
			Matcher matcher = PopConnection.retrPat.matcher(line);
			matcher.find();
			int index = 0;
	        try {
	        	index = (new Integer(matcher.group(1))).intValue();
	        } catch (Exception e) {
	        	this.sendErr("Not a valid index number A -- " + matcher.group(1));
	        	return false;
	        }
	        try {
	        	int n = this.myMails.size();
	        	if (index == 0) {
	        		this.sendErr("Not a valid index number B");
	        		return false;
	        	} else if ((index > n) || (index < 1)) {
	        		this.sendErr("Not a valid index number C");
	        		return false;
	        	} else if (n == 0) {
	        		this.sendErr("Not a valid index number D");
	        		return false;
	        	}
	        	this.sendOk("there it goes");
	        	this.out.print(this.myGmailContact.fetchOriginalMail(((GMConversationEntry)this.myMails.get(index - 1)).getId()).trim() + "\r\n");
	        	this.out.print(".\r\n");
	        	this.out.flush();
	        	return false;
			}catch(IOException e) {
				this.sendErr("There was an error in the request sent");
				return false;
			}
		}		//TOP
		else if (topPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("not in transaction state");
				return false;
			}
			Matcher matcher = topPat.matcher(line);
			matcher.find();
			int msg = 0;
			int lines = 0;
			try {
				msg = Integer.parseInt(matcher.group(1));
				lines = Integer.parseInt(matcher.group(2));
			} catch (Exception e) {
				System.out.println(msg + " was passed"  + line);
				System.out.println(lines + " was passed " + line);
				sendErr("Not a valid index number A -- " + matcher.group(1));
				return false;
			}
			try {
				this.sendOk("there it goes");
				String whole[] = this.myGmailContact.fetchOriginalMail(((GMConversationEntry)this.myMails.get(msg - 1)).getId()).trim().split("\n");
				for(int i = 0 ;i < whole.length; i++) {
					if(whole[i].equals("\\r\\n\\r\\n"))
						break;
					this.out.print(whole[i] + "\r\n");
				}
				this.out.print(".\r\n");
				this.out.flush();
				return false;
			}catch(IOException e) {
				this.sendErr("There was an error in the request sent");
				return false;
			}
		}	//UIDX
		else if (PopConnection.uidlNumPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("Not in transaction state");
				return false;
			}
			int index = 0;
			Matcher matcher = PopConnection.uidlNumPat.matcher(line);
			matcher.find();
			try {
				index = (new Integer(matcher.group(1))).intValue();
			} catch (Exception e) {
				this.sendErr("Not a valid index number A -- " + matcher.group(1));
				return false;
			}
			int n = this.myMails.size();
			if (index == 0) {
				this.sendErr("Not a valid index number B");
				return false;
			} else if ((index > n) || (index < 1)) {
				this.sendErr("Not a valid index number C");
				return false;
			} else if (n == 0) {
				this.sendErr("Not a valid index number D");
				return false;
			}
			try {
				this.sendOk(index + " " + ((GMConversationEntry)this.myMails.get(index)).getId());
			} catch (Exception e) {
				this.sendErr("Not implemented by this module");
			}
			return false;
		}		//UIDL
		else if (PopConnection.uidlPat.matcher(line).find()) {
			if (this.state != PopConnection.TRANSACTION) {
				this.sendErr("Not in transaction state");
				return false;
			}
			int numMessages = this.myMails.size();
			if (numMessages == 0) {
				this.sendOk("");
				this.out.print(".\r\n");
				this.out.flush();
				return false;
			}
			this.sendOk("");
			int k = 0;
			for(int i = 0; i < this.myMails.size(); i++)
				this.out.print((i + 1) + " " + ((GMConversationEntry)this.myMails.get(i)).getId() + "\r\n");
			this.out.print(".\r\n");
			this.out.flush();
			return false;
		}	//RSET
		else if (PopConnection.rsetPat.matcher(line).find()) {
			this.sendOk("");
			return false;
		}else {
			this.sendErr("Command not supported or recognized");
		}
		return false;
	}
}
