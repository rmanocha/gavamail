/*
 *	Created on Nov 19, 2004
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

import java.util.Properties;

import java.io.IOException;
import java.io.FileInputStream;
/**
 * @author rmanocha
 *
 */
public class ReadOpts {
	
	private transient Properties myConfFile;
	private transient String username;
	private transient String password;
	private transient String[] label;
	private transient String mdir;
	private transient String unreadOnly;
	private transient String useMySQL;
	private transient String mysql_user;
	private transient String mysql_pass;
	private transient String mysql_database;
	private transient String mysql_host; 
	
	public ReadOpts(String filename) throws ReadOptsException {
		this.myConfFile = new Properties();
		try {
			this.myConfFile.load(new FileInputStream(filename));
		} catch(IOException ex) {
			throw new ReadOptsException("No configuration file " + filename + " found.");
		}
		this.read();
	}
	
	private void read() throws ReadOptsException {
		this.username = this.myConfFile.getProperty("username");
		this.password = this.myConfFile.getProperty("password");
		String labels = this.myConfFile.getProperty("label", "inbox");
		this.mdir = this.myConfFile.getProperty("baseMdir");
		this.unreadOnly = this.myConfFile.getProperty("unread-only","false");
		if(labels.equals(""))
			labels = "inbox";
		this.label = labels.split(",");
		for(int i = 0; i < label.length; i++)
			this.label[i].trim();
		if((this.username.equals("")) || (this.password.equals("")) || (this.mdir.equals("")))
			throw new ReadOptsException("The configuration files does not specify one, two or all of username, password or base maildir directory.");
		//From here on starts the mysql stuff...
		this.useMySQL = this.myConfFile.getProperty("use-mysql","false");
		if(useMySQL.equalsIgnoreCase("true")) {
			this.mysql_user = this.myConfFile.getProperty("mysql-user");
			this.mysql_pass = this.myConfFile.getProperty("mysql-pass");
			this.mysql_database = this.myConfFile.getProperty("mysql-db");
			this.mysql_host = this.myConfFile.getProperty("mysql-host");
			if((this.mysql_user.equals("")) || (this.mysql_pass.equals("")) || (this.mysql_database.equals("")) || (this.mysql_host.equals("")))
				throw new ReadOptsException("The configuration asked to use a MySQL database either the username, password of the datbase name was not specified in the configuration file. Please correct this.");
		}
	}
	
	public String getUsername() {	return this.username;	}
	
	public String getPassword() {	return this.password;	}
	
	public String[] getLabel() {	return this.label;	}
	
	public String getBaseDir() {	return this.mdir;	}
	
	public boolean getUnread() {
		if(this.unreadOnly.equalsIgnoreCase("true"))
			return true;
		return false;
	}
	
	public boolean getUseMySql() {
		if(this.useMySQL.equalsIgnoreCase("true"))
			return true;
		return false;
	}
	
	public String getMySqlUser() {	return this.mysql_user;	}
	
	public String getMySqlPass() {	return this.mysql_pass;	}
	
	public String getMySqlDatabase() {	return this.mysql_database;	}
	
	public String getMySqlHost() {	return this.mysql_host;	}
}