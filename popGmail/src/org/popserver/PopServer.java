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

import java.io.IOException;

import EDU.oswego.cs.dl.util.concurrent.Executor;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

public class PopServer extends Thread {
	private static Logger logger = Logger.getLogger("org.popserver.PopServer");
	public static final int DEFAULT_PORT = 11110;
	public static final int DEFAULT_BUFFERLENGTH = 10;
	public static final int DEFAULT_MAXIMUM_THREADS = 10;
	public static final int DEFAULT_MINIMUM_THREADS = 5;
	public static final int DEFAULT_KEEPALIVE = 1000 * 60 * 5;
	protected int port;
	protected ServerSocket listenSocket;
	private Executor executor;
	public boolean carryon = true;
	private boolean noRemote;
	private Preferences myPrefs = Preferences.userNodeForPackage(PopServer.class);
	
	public PopServer(int port, boolean noRemote) throws IOException {
		this.port = port;
		this.noRemote = noRemote;
		PopServer.logger.info("Starting POP server on port: " + this.port);
		
		this.executor = new PooledExecutor(new BoundedBuffer(PopServer.DEFAULT_BUFFERLENGTH), PopServer.DEFAULT_MAXIMUM_THREADS);
		((PooledExecutor) this.executor).setMinimumPoolSize(PopServer.DEFAULT_MINIMUM_THREADS);
		((PooledExecutor) this.executor).setKeepAliveTime(PopServer.DEFAULT_KEEPALIVE);
		((PooledExecutor) this.executor).abortWhenBlocked();
		((PooledExecutor) this.executor).createThreads(PopServer.DEFAULT_MINIMUM_THREADS);
        
		try {
			this.listenSocket = new ServerSocket(this.port);
		} catch (Exception e) {
			System.err.println("You tried to open a port which you are not allowed to open. Please use valuse above 8000.");
			System.exit(1);
		}
		this.start();
		PopServer.logger.info(" PopServer started ");
	}
	
	public void run() {
		Socket clientSocket;
		while (!this.myPrefs.getBoolean("stop",false)) {
		    System.out.println(this.myPrefs.getBoolean("stop",false));
			try {
				clientSocket = this.listenSocket.accept();
			} catch (SocketException ex) {
				if (this.myPrefs.getBoolean("stop",false)) { 
					PopServer.logger.log(Level.SEVERE, "Should not happen! Exiting from server. ", ex);
					this.carryon = false;
				}
				break;
			} catch (IOException ex) {
				PopServer.logger.log(Level.SEVERE, "Should not happen! Exiting from server. ", ex);
				break;
			}
			PopServer.logger.info("Someone connected");
			try {
				if (this.isSocketAllowed(clientSocket)) {
					PopServer.logger.info("Connection accepted for IP " + clientSocket.getInetAddress());
					//try {
						//this.executor.execute(new PopConnection(clientSocket));
						Thread newThread = new Thread(new PopConnection(clientSocket));
						newThread.run();
						PopServer.logger.info("run() done");
					/*} catch (InterruptedException e) {
						PopServer.logger.warning("Interrupted exception for IP: " + clientSocket.getInetAddress());
					} catch (PooledExecutor.AbortException e) {
						logger.warning("Too many connections! IP trying to connect was " + clientSocket.getInetAddress());
						clientSocket.close();
					}*/
				} else {
					PopServer.logger.warning("Connection not allowed for remote IP " + clientSocket.getInetAddress());
					clientSocket.close();
				}
			} catch (IOException e) {
				PopServer.logger.log(Level.FINE, " client socket closed with difficulty. ", e);
			}
		}
		System.out.println("here1");
		this.shutdown();
	}
	
	private boolean isSocketAllowed(Socket socket) {
		if(this.noRemote) {
			if(socket.getLocalAddress().equals(socket.getInetAddress()))
				return true;
			else
				return false;
		}
	 	return true;
	}
	
	public void shutdown() {
	    System.out.println("here");
		PopServer.logger.info("Shutting down the POP3 Server");
		PopServer.logger.fine("Halt POP3 Server thread");
		//this.carryon = false;
		this.interrupt();
		try {
			if (this.listenSocket != null) {
				this.listenSocket.close();
			}
		} catch (IOException e) {
			PopServer.logger.fine("Not important: IOException while closing finally ListenSocket");
		}
		this.listenSocket = null;
		((PooledExecutor)this.executor).shutdownAfterProcessingCurrentlyQueuedTasks();
		PopServer.logger.info("POP3 Server shut down");
	}
	
	//public static Preferences getPrefs() {	return PopServer.myPrefs;	}
}
