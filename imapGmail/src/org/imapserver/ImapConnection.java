/*
 *	Created on Oct 25, 2004
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
package org.imapserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.File;

import java.net.Socket;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

import org.util.*;

/**
 * @author rmanocha
 *
 */
public class ImapConnection implements Runnable {
	
	private static Logger logger = Logger.getLogger("org.imapserver.ImapConnection");
	private static final byte NotAuthenticated = 0;
	private static final byte Authenticated = 1;
	private static final byte Selected = 2;
	private static final byte Logout = 3;
	private byte state;
	private boolean have_username = false;
	private String login;
	private String password;
	protected BufferedReader in;
	protected PrintWriter out;
	protected Socket myClient;
	private String myUser;
	private String myPassword;
	private ArrayList myMailBoxes;
	private int mySelectedMailBox;
	private ReadOpts myOpts;
	
	public ImapConnection(Socket socket) {
		this.setSocket(socket);
		try {
			this.myOpts = new ReadOpts(".mapgmailrc");
		} catch (ReadOptsException e) {
			e.printStackTrace();
			System.exit(1);
		}
		this.myMailBoxes = new ArrayList();
		this.populateMailboxes();
	}
	
	private void populateMailboxes() {
		File tmp = new File(this.myOpts.getBaseDir());
		if(!tmp.exists()) {
			System.err.println("The specified base maildir directory: " + tmp.getName() + " does not exist. Please correct this.");
			System.exit(2);
		}
		String[] labels = tmp.list();
		for(int i = 0; i < labels.length; i++) {
			if((!labels[i].equals("new")) && (!labels[i].equals("cur")) && (!labels[i].equals("tmp"))) {
				try {
					this.myMailBoxes.add(new ImapMailbox(labels[i],this.myOpts.getBaseDir() + labels[i] + File.separator));
				} catch (IOException e) {	e.printStackTrace();	}
			}
		}
		try {
			this.myMailBoxes.add(new ImapMailbox("inbox", this.myOpts.getBaseDir()));
		} catch (IOException e) {	e.printStackTrace();	}
	}
	
	private void setSocket(Socket socket) {
		this.myClient = socket;
		try {
			this.in = new BufferedReader(new InputStreamReader(this.myClient.getInputStream()));
			this.out = new PrintWriter(this.myClient.getOutputStream(),true);
		} catch (IOException e) {
			ImapConnection.logger.log(Level.SEVERE, "should not happen", e);
       	}
		this.state = ImapConnection.NotAuthenticated;
	}
	
	private void resetSocket() throws IOException {
        if (this.myClient != null) {
           	this.myClient.close();
        }
        ImapConnection.logger.log(Level.WARNING, "Inside resetSocket()");
        this.in = null;
        this.out = null;
	}
	
	public void run() {
		try {
           	String line;
           	boolean done = false;
           	this.sendOkUnTagged("OK gavamail IMAP gmail proxy ready");
           	ImapConnection.logger.info("Got into run");
           	while (!done) {
           		ImapConnection.logger.info("Inside while loop");
           		line = this.in.readLine();
           		if (line != null) {
           			ImapConnection.logger.info(line + " was passed");
           			done = this.parseLine(this.getEachCommand(line));
           		} else {
           			ImapConnection.logger.log(Level.INFO, "client socket closed without QUIT");
           			done = true;
           		}
           	}
        } catch (Exception e) {
        	ImapConnection.logger.log(Level.SEVERE, "should not happen", e);
        } finally {
        	try {
        		this.resetSocket();
			} catch (Exception e) {
				ImapConnection.logger.log(Level.SEVERE, "should not happen", e);
			}
		}
	}
	
	private void sendOkUnTagged(String line) {
		this.out.print("* OK " + line + "\r\n");
		ImapConnection.logger.fine("* OK " + line);
		this.out.flush();
	}
	
	private void sendOkTagged(String tag, String command, String line) {
		this.out.print(tag + " OK " + command + line + "\r\n");
		ImapConnection.logger.fine(tag + " OK " + line);
		this.out.flush();
	}
	
	private void sendBadUnTagged(String line) {
		this.out.println("* " +  line + "\r\n");
		ImapConnection.logger.fine("* " + line);
		this.out.flush();
	}
	
	private void sendBadTagged(String tag, String command, String line) {
		this.out.print(tag + " BAD " + command + line + "\r\n");
		ImapConnection.logger.fine(tag + " BAD " + line);
		this.out.flush();
	}
	
	private void sendNoTagged(String tag, String command, String line) {
		this.out.print(tag + " NO " + command + line +"\r\n");
		ImapConnection.logger.fine(tag + " NO " + line);
		this.out.flush();
	}
	
	private void sendNoUnTagged(String line) {
		this.out.print("* NO " + line + "\r\n");
		ImapConnection.logger.fine("* NO " + line);
		this.out.flush();
	}
	
	private void sendPlus(String line) {		//Need to send space in line
		this.out.print("+" + line);
		ImapConnection.logger.fine("+" + line);
		this.out.flush();
	}
	
	private void sendUnTagged(String line) {
		this.out.print("* " + line + "\r\n");
		ImapConnection.logger.fine("* " + line + "\r\n");
		this.out.flush();
	}
	
	private String[] getEachCommand(String line) {
		String[] whole = line.split("\\s");
		return whole;
	}
	
	private boolean parseLine(String[] wholeCommand) {
		if(wholeCommand.length <= 1) {
			this.sendBadUnTagged("Syntax error");
			return false;
		}
		if(wholeCommand[1].equalsIgnoreCase("CAPABILITY")) {			//No AUTH= sent since we want to use LOGIN
			ImapConnection.logger.info("Inside 1");
			this.sendUnTagged("CAPABILITY IMAP4rev1");
			this.sendOkTagged(wholeCommand[0],wholeCommand[1]," Completed");
			return false;
		} else if(wholeCommand[1].equalsIgnoreCase("authenticate")) {	//shouldnt really matter since I Dont send any AUTH= in CAPABILITY
			if(wholeCommand[2].equalsIgnoreCase("plain")) {
				this.sendPlus("");
			}
			else
				this.sendNoTagged(wholeCommand[0], wholeCommand[1], " Authentication mechanism not supported.");
			return false;
		}
		/**
		 * TODO: Find a way to authenticate.
		 */
		else if(wholeCommand[1].equalsIgnoreCase("login")) {			//works fine
			if(this.state != ImapConnection.NotAuthenticated) {
				this.sendNoUnTagged("Client not authenticated.");
				this.sendNoTagged(wholeCommand[0],wholeCommand[1], " not in authenticated state");
				return false;
			}
			this.state = ImapConnection.Authenticated;
			this.sendOkTagged(wholeCommand[0],wholeCommand[1]," completed");
			return false;
		} else if(wholeCommand[1].equalsIgnoreCase("list")) {
			if(this.state != ImapConnection.Authenticated) {
				this.sendNoUnTagged("Not Authenticated");
				this.sendNoTagged(wholeCommand[0],wholeCommand[1], " not in authenticated state");
				return false;
			}
			if(wholeCommand[2].equals("\"\"") && wholeCommand[3].equals("\"\"")) {
				this.sendUnTagged(wholeCommand[1] + ("(\\Noselect) \"/\" \"\""));
				this.sendOkTagged(wholeCommand[0], wholeCommand[1], " Completed");
				return false;
			}
			return false;
		} else if(wholeCommand[1].equalsIgnoreCase("lsub")) {
			if(this.state != ImapConnection.Authenticated) {
				this.sendNoUnTagged("Not Authenticated");
				this.sendNoTagged(wholeCommand[0],wholeCommand[1], " not in authenticated state");
				return false;
			}
			if(wholeCommand[2].equals("\"\"") && wholeCommand[3].equals("\"*\"")) {
				this.sendUnTagged(wholeCommand[1] + ("(\\Noselect) \"/\" \"\""));
				this.sendOkTagged(wholeCommand[0], wholeCommand[1], " Completed");
				return false;
			}
			return false;
		} else if(wholeCommand[1].equalsIgnoreCase("select")) {
			if(this.state != ImapConnection.Authenticated) {
				this.sendNoUnTagged("Not Authenticated");
				this.sendNoTagged(wholeCommand[0],wholeCommand[1], " not in authenticated state");
				return false;
			}
			String mboxName = wholeCommand[2].replace('"',' ').trim();	//Because the name has quotes on both sides...:@
			boolean selected = false;
			for(int i = 0;i < this.myMailBoxes.size();i++) {
				if(((ImapMailbox)this.myMailBoxes.get(i)).getLabel().equalsIgnoreCase(mboxName)) {
					this.mySelectedMailBox = i;
					selected = true;
					break;
				}
			}
			if(!selected) {
				this.sendNoUnTagged("Mailbox name does not exist.");
				this.sendNoTagged(wholeCommand[0],wholeCommand[1]," Mailbox does not exist error.");
				return false;
			}
			System.out.println("selected");
			ImapMailbox selectedBox = (ImapMailbox)this.myMailBoxes.get(this.mySelectedMailBox);
			this.sendUnTagged(Integer.toString(selectedBox.getNumTotal()) + " EXISTS");
			System.out.println(Integer.toString(selectedBox.getNumTotal()) + " EXISTS");
			this.sendUnTagged(Integer.toString(selectedBox.getNumNew()) + " RECENT");
			if(selectedBox.hasNew()) 
				this.sendOkUnTagged("[UNSEEN 1] Message 1 is first unseen");
			else
				this.sendOkUnTagged("[UNSEEN 0] There are no unseen Messages");
			this.sendUnTagged("[FLAGS] ()");			//no flags are supported(for now).
			this.sendOkUnTagged("[PERMANENTFLAGS ()] Limited");
			wholeCommand[1] = "[READ-ONLY]" + wholeCommand[1];
			this.sendOkTagged(wholeCommand[0], wholeCommand[1], " Completed");
			return false;
		}
		return false;
	}
}
