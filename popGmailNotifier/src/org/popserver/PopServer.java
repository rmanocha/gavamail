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


/**
 * @author rmanocha
 *
 * This class implements the PopServer. It creates a PopConnection Object which then takes over to do the remaining of the work.
 */
public class PopServer extends Thread {
	private static Logger logger = Logger.getLogger("org.popserver.PopServer");
    public static final int DEFAULT_PORT = 11112;
	public static final int DEFAULT_BUFFERLENGTH = 10;
    public static final int DEFAULT_MAXIMUM_THREADS = 10;
    public static final int DEFAULT_MINIMUM_THREADS = 5;
    public static final int DEFAULT_KEEPALIVE = 1000 * 60 * 5;
    protected int port;
	protected ServerSocket listenSocket;
	private Executor executor;
	public boolean carryon = true;
	
	/**
	 * Constructor for a PopServer object.
	 * @param port	The port on which the connection shoule be opened. Default is 11112.
	 * @throws IOException	Throws an IOException if there is an error in opening and then reading and writing to the socket.
	 */
	public PopServer(int port) throws IOException {
		this.port = port;
		PopServer.logger.info("Starting POP server on port: " + this.port);
		this.executor = new PooledExecutor(new BoundedBuffer(PopServer.DEFAULT_BUFFERLENGTH), PopServer.DEFAULT_MAXIMUM_THREADS);
       	((PooledExecutor) this.executor).setMinimumPoolSize(PopServer.DEFAULT_MINIMUM_THREADS);
       	((PooledExecutor) this.executor).setKeepAliveTime(PopServer.DEFAULT_KEEPALIVE);
       	((PooledExecutor) this.executor).abortWhenBlocked();
       	((PooledExecutor) this.executor).createThreads(PopServer.DEFAULT_MINIMUM_THREADS);

		this.listenSocket = new ServerSocket(this.port);
		this.start();
		PopServer.logger.info(" PopServer started ");
	}
	
	/**
	 * Run method since this class implements a Thread.
	 */
	public void run() {
       	Socket clientSocket;
       	while (this.carryon) {
           	try {
           		clientSocket = listenSocket.accept();
           	} catch (SocketException ex) {
        		if (this.carryon) { 
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
            	if (isSocketAllowed(clientSocket)) {
               		PopServer.logger.info("Connection accepted for IP " + clientSocket.getInetAddress());
               		try {
                   		this.executor.execute(new PopConnection(clientSocket));
               		} catch (InterruptedException ex) {
                   		PopServer.logger.warning("Interrupted exception for IP: " + clientSocket.getInetAddress());
		            }catch (PooledExecutor.AbortException ex) {
                   		logger.warning("Too many connections! refused for IP  " + clientSocket.getInetAddress());
                   		clientSocket.close();
               		}
               	} else {
                	PopServer.logger.warning("Connection not allowed for IP " + clientSocket.getInetAddress());
                	clientSocket.close();
               	}	
            } catch (IOException e) {
            	PopServer.logger.log(Level.FINE, " client socket close with difficulty. ", e);
            }
        }
    }
	
	private boolean isSocketAllowed(Socket socket) {
		return true;
	}
	
	private void shutdown() {
      	PopServer.logger.info("Shutting down pop server");
      	PopServer.logger.fine("Halt popserver thread");
      	this.carryon = false;
      	this.interrupt();
      	try {
        	if (this.listenSocket != null) {
        		this.listenSocket.close();
            }
        } catch (IOException e) {
            PopServer.logger.fine(" Not important: IOException while closing finally ListenSocket");
        }
      	this.listenSocket = null;
      	((PooledExecutor)this.executor).shutdownAfterProcessingCurrentlyQueuedTasks();
      	PopServer.logger.info("pop server shut down");
    }
}
