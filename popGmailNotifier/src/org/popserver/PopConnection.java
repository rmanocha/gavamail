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
import siuying.gm.structure.GMThread;
import siuying.gm.ParsePacketException;


/**
 * @author rmanocha
 *
 * This class is used to actually make the connection with the incoming socket connection from the Client.
 * Since it implements Runnable, the run() method is called when called from a Thread.
 */
public class PopConnection implements Runnable {

	private static Logger logger = Logger.getLogger("org.popserver.PopConnection");
	private static Pattern userPat = Pattern.compile("\\AUSER\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static Pattern passPat = Pattern.compile("\\APASS\\s+(.+)", Pattern.CASE_INSENSITIVE);
	private static Pattern quitPat = Pattern.compile("\\AQUIT\\s*\\Z", Pattern.CASE_INSENSITIVE);
	private static Pattern statPat = Pattern.compile("\\ASTAT\\s*\\Z", Pattern.CASE_INSENSITIVE);
	private static Pattern uidlPat = Pattern.compile("\\AUIDL\\s*\\Z", Pattern.CASE_INSENSITIVE);
	private static Pattern uidlNumPat = Pattern.compile("\\AUIDL\\s+([0-9]+)", Pattern.CASE_INSENSITIVE);
	private static final byte RESET = 0;
	private static final byte AUTHORIZATION = 1;
	private static final byte TRANSACTION = 2;
	private static final byte UPDATE = 3;
	private byte state;
	private boolean have_username = false;
	private String login;
	private String password;
	private String usernameExtension;
	protected BufferedReader in;
	protected PrintWriter out;
	protected Socket myClient;
	private GMConnector myGmailContact;
	private int totalNumber;
	private int totalNew;
	
	/**
	 * constructor for a PopConnection Object. It sets the socket which will be used to talk with the client.
	 * @param socket The socket on which the client/server are listening for commands.
	 */
	public PopConnection(Socket socket) {
		this.setSocket(socket);
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
	
	private void resetSocket() throws IOException {
       	if (this.myClient != null) {
           	this.myClient.close();
       	}
       	this.myClient = null;
       	this.in = null;
       	this.out = null;
       	this.state = PopConnection.RESET;
    }
	
	/**
	 * The run method for the Runnable interface. It goes into a loop which only breaks when get Quit from the client.
	 */
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
          	} catch (Exception e) {
           		PopConnection.logger.log(Level.SEVERE, "should not happen", e);
           	}
        }
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
	
	private boolean parseLine(String line) {
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
				this.totalNumber = Integer.parseInt((String)tree.get("current.box.totalthreads"));
				int numThreadspp = Integer.parseInt((String)tree.get("current.box.threadsperpage"));
				int numPages = this.totalNumber / numThreadspp;
				if((this.totalNumber % numThreadspp) != 0)
					numPages ++;
				boolean flag = false;
				for(int i = 0; i < numPages; i++) {
					myResponse = this.myGmailContact.request(siuying.gm.GMConstants.GM_REQ_STANDARD,"ALL",Integer.toString(i));
            		ArrayList myThreads = myResponse.getGMThreads();
					for(int j = 0; j < myThreads.size();j++) {
						if(!((GMThread)myThreads.get(j)).isRead()) 
							this.totalNew++;
						else {
							flag = true;
							break;
						}
					}
					if(flag)
						break;
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
			this.sendOk("Good Bye");
            return true;
		}	//STAT		This should work with myltiple conversationEntries
		else if (PopConnection.statPat.matcher(line).find()) {
            if (this.state != PopConnection.TRANSACTION) {
            	this.sendErr("not in transaction state");
            	return false;
            }
            this.sendOk(this.totalNew + " " + this.totalNew*100);
            return false;
		} 	//UIDL X
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
			try {
            	this.sendOk(index + " " + this.totalNew*100);
            } catch (Exception e) {
            	this.sendErr("Not implemented by this module");
            }
            return false;
		}else {
           	this.sendErr("Command not supported or recognized");
        }
		return false;
	}
}
