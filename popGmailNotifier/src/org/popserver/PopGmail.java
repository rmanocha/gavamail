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

import java.util.logging.Logger;


/**
 * @author rmanocha
 *
 * This is the class containg the main method for the Server. It starts a @see PopServer on port 11113.
 */
public class PopGmail {

	private static Logger logger = Logger.getLogger("org.popserver.PopGmail");
	
	/**
	 * Constructor for a PopGmail object.
	 *
	 */
	public PopGmail() {
		try {
			PopServer myServer = new PopServer(11113);
		}catch (Exception e) {	e.printStackTrace();	}
	}
	
	public static void main(String[] args) {
		PopGmail.logger.info("Checking for unread messages only");
		PopGmail gmail = new PopGmail();
		PopGmail.logger.info("Finished starting application");
		
	}
}
