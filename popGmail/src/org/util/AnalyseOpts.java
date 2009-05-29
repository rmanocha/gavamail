/*
 *	Created on Oct 24, 2004
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
package org.util;

import java.util.logging.Logger;
/**
 * 
 * This class analyses the options parsed to the program. It then returns these options through public access methods.
 */
public class AnalyseOpts {
	
	private boolean unread_only;
	private boolean localhost_only;
	public boolean askStop;
	private int port = 11112;
	private static Logger logger = Logger.getLogger("org.util.AnalyseOpts");
	
	/**
	 * Constructor for the AnalyseOpts Object. Accepts an array of options parsed by the user and analyses them.
	 * @param args
	 */
	public AnalyseOpts(String[] args) {
		if(args.length > 5) {
			System.err.println("Too many arguments passed.");
			System.err.println(this.syntax());
			System.exit(1);
		}
		boolean parsedPort = false;
		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("--unread-only")) {
				if(this.unread_only) {
					System.err.println("You have passed the --unread-only option twice. I am exiting.");
					System.err.println(this.syntax());
					System.exit(1);
				}
				this.unread_only = true;
				AnalyseOpts.logger.info("Polling for unread messages only.");
			}
			else if(args[i].equals("--port")) {
				if(parsedPort) {
					System.err.println("You have passed the --port option twice. Please correct this.");
					System.err.println(this.syntax());
					System.exit(1);
				}
				parsedPort = true;
				try {
					this.port = Integer.parseInt(args[++i]);
				} catch(Exception e) {
					System.err.println("An integer value is required after the --port option. Please correct this.");
					System.err.println(this.syntax() + "\n");
					e.printStackTrace();
					System.exit(1);
				}
			}
			else if(args[i].equals("--localhost-only")){
				if(this.localhost_only) {
					System.err.println("You passed the --localhost-only option twice. I am exiting.");
					System.err.println(this.syntax());
					System.exit(1);
				}
				this.localhost_only = true;
				AnalyseOpts.logger.info("Allowing local connections only.");
			}
			else if(args[i].equals("--help")) {
				System.out.println(this.syntax());
				System.exit(0);
			}
			else if(args[i].equals("--stop")) {
			    this.askStop = true;
			    break;
			}
			else {
				System.err.println("You have passed an unrecognised option.");
				System.err.println(this.syntax());
				System.exit(1);
			}
		}
		//AnalyseOpts.logger.info("Starting the POP3 server on port: " + this.port);
	}
	
	/**
	 * Returns a boolean specifying if we need to poll for unread messages only.
	 * @return boolean Indicating polling for unread messages only
	 */
	public boolean getUnreadOnly() {	return this.unread_only;	}
	
	/**
	 * Returns a boolean specifying if we should allow connections from localhost only.
	 * @return boolean Indicating if connections from localhost should be allowed only.
	 */
	public boolean getLocalhostOnly() {	return this.localhost_only;	}
	
	/**
	 * Returns the port on which we should start the server.
	 * @return int Port on which we should start the server. Defaults to 11112.
	 */
	public int getPort() {	return this.port;	}
	
	public boolean needStop() {	return this.askStop;	}

	private String syntax() {
		String syntax = "PopGavaMail USAGE: popgavamail [options]\nOptions:";
		syntax += "\n--unread-only\t\tFetch Unread messages only. Defaults to false.";
		syntax += "\n--localhost-only\tAllow connections from localhost only. Defaults to false.";
		syntax += "\n--port [num]\t\tStart the POP3 server on port \"num\". Defaults to 11112.";
		syntax += "\n\n(c) Rishabh Manocha 2004-2005\nReleased under the terms of the GPL v2. See COPYING for more info.";
		return syntax;
	}
}
