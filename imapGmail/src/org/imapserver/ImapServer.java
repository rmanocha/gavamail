/*
 *	Created on Oct 27, 2004
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author rmanocha
 *
 */
public class ImapServer extends Thread{
	
	private int port;
	private static Logger logger = Logger.getLogger("org.imapserver.ImapServer");
	public static final int DEFAULT_PORT = 11110;
	public static final int DEFAULT_BUFFERLENGTH = 10;
    public static final int DEFAULT_MAXIMUM_THREADS = 10;
    public static final int DEFAULT_MINIMUM_THREADS = 5;
    public static final int DEFAULT_KEEPALIVE = 1000 * 60 * 5;
    protected ServerSocket listenSocket;
	public boolean carryon = true;
	private boolean noRemote;
	
	public ImapServer(int port) {
		this.port = port;
		ImapServer.logger.info("Starting Imap server on port: " + this.port);
		
        try {
        	this.listenSocket = new ServerSocket(this.port);
        } catch (Exception e) {
        	System.err.println("You tried to open a port which you are not allowed to open. Please use valuse above 8000.");
        	System.exit(1);
        }
		this.start();
	}
	
	public void run() {
    	Socket clientSocket;
    	while (this.carryon) {
           	try {
           		clientSocket = this.listenSocket.accept();
           	} catch (SocketException ex) {
           		if (this.carryon) { 
               		ImapServer.logger.log(Level.SEVERE, "Should not happen! Exiting from server. ", ex);
               		this.carryon = false;
           		}
           		break;
           	} catch (IOException ex) {
           		ImapServer.logger.log(Level.SEVERE, "Should not happen! Exiting from server. ", ex);
           		break;
           	}
           	ImapServer.logger.info("Someone connected");
           	try {
           		if (this.isSocketAllowed(clientSocket)) {
           			ImapServer.logger.info("Connection accepted for IP " + clientSocket.getInetAddress());
           			Thread newThread = new Thread(new ImapConnection(clientSocket));
					newThread.run();
           		}
           		else {
                    ImapServer.logger.warning("Connection not allowed for IP " + clientSocket.getInetAddress());
                    clientSocket.close();
                }
            } catch (IOException e) {
            	ImapServer.logger.log(Level.FINE, " client socket close with difficulty. ", e);
            }
    	}
    }
	
	private boolean isSocketAllowed(Socket socket) {	return true;	}
}
