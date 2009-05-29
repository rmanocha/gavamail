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
import org.util.AnalyseOpts;

public class PopGmailStart {

	private static Logger logger = Logger.getLogger("org.popserver.PopGmailStart");
	
	public PopGmailStart(String[] args) {
		AnalyseOpts parsedOpts = new AnalyseOpts(args);
		PopConnection.unreadOnly = parsedOpts.getUnreadOnly();
		try {
			PopServer myServer = new PopServer(parsedOpts.getPort() , parsedOpts.getLocalhostOnly());
		}catch (Exception e) {	e.printStackTrace();	}
			PopGmailStart.logger.info("Finished starting PopGmail");
	}
	
	public static void main(String[] args) {
		PopGmailStart gmail = new PopGmailStart(args);
	}
}
