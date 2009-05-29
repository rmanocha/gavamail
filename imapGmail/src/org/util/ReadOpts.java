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
	private transient String mdir;
	
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
		this.mdir = this.myConfFile.getProperty("baseMdir");
	}
	
	public String getBaseDir() {	return this.mdir;	}
	
}
