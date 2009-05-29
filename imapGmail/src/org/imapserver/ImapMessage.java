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
package org.imapserver;

/**
 * @author rmanocha
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ImapMessage {

	private int myNumber;
	private long myUidVal;		//Unique Identifier Value
	private long myUidVerVal;	//Unique Identifier Validity Value
	private boolean[] myFlag;	//an array for the Message flags(\Seen,\Answered,\Flagged,\Deleted,\Draft,\Recent) in this order.
	private int myIntDate;		//Internal Date Message Attribute
	private long mySize;		//Internal size(in octets) Attribute.
	private String myEnvStruct;	//Envelope Structure Message Attribute
	
	public ImapMessage() {
		
	}
}
