/*
 *	Created on Nov 24, 2004
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

/**
 * @author rmanocha
 *
 */
public class AnalyseOpts {
	
	private boolean readFromCommandLine;
	private boolean helpAsked;
	private String myConfFile;
	private String myWrittenFile;
	private String myUsername;
	private String myPassword;
	private String myRootDir;
	private String myLabel[];
	private boolean unreadOnly;
	
	public AnalyseOpts(String[] args) throws AnalyseOptsException{
		this.myConfFile = ".mdirgmailrc";
		this.myWrittenFile = ".mdirgmailwritten";
		this.analyse(args);
	}
	
	private void analyse(String[] args) throws AnalyseOptsException {
		try {
			if(args[0].equals("--help"))
				this.helpAsked = true;
			else if(args[0].equals("--conf")) {
				try {
					this.myConfFile = args[1];
				} catch (ArrayIndexOutOfBoundsException e) {
					throw new AnalyseOptsException("You did not specify a configuration file to read from(but you passed the --conf option");
				}
				try {
					if(args[2].equals("--written")) {
						try {
							this.myWrittenFile = args[3];
						} catch(ArrayIndexOutOfBoundsException e) {
							throw new AnalyseOptsException("You did not specify a written file to read from(but you passed the --written option");
						}
					}
					else
						throw new AnalyseOptsException("You passed an unrecognised options: " + args[2]);
				} catch(ArrayIndexOutOfBoundsException e) {	}
			}
			else {
				this.readFromCommandLine = true;
				try {
					this.myUsername = args[0];
					this.myPassword = args[1];
					this.myRootDir = args[2];
					this.myLabel = args[3].split(",");
				} catch(ArrayIndexOutOfBoundsException e) {
					throw new AnalyseOptsException("You did not pass a required option.");
				}
				try {
					for(int i = 4; i < 7; i++) {
						if(args[i].equals("--written")) {
							try {
								this.myWrittenFile = args[++i];
							} catch(ArrayIndexOutOfBoundsException e) {
								throw new AnalyseOptsException("You did not specify a written file to read from(but you passed the --written option)");
							}
						}
						else if (args[i].equals("--unread-only")) {
							this.unreadOnly = true;
						}
						else
							throw new AnalyseOptsException("Unrecognised options: " + args[i]);
					}
				} catch(ArrayIndexOutOfBoundsException e) { }
			}
		} catch(ArrayIndexOutOfBoundsException e) { }
	}
	
	public boolean gotHelp() {	return this.helpAsked;	}
	
	public boolean readCommandLine() {	return this.readFromCommandLine;	}
	
	public String getUser() {	return this.myUsername;	}
	
	public String getPassword() {	return this.myPassword;	}
	
	public String getRootDir() {	return this.myRootDir;	}
	
	public String[] getLabel() {	return this.myLabel;	}
	
	public String getConfFile() {	return this.myConfFile;	}
	
	public String getWrittenFile() {	return this.myWrittenFile;	}
	
	public boolean getUnreadOnly() {	return this.unreadOnly;	}

}
