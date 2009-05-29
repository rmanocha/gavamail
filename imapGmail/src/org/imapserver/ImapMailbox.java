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

import java.io.IOException;
import java.io.File;

/**
 * @author rmanocha
 * 
 */
public class ImapMailbox {
	
	private String myLabel;			//the label whose messages this mailbox will read.
	private int myTotalNum;
	private int myTotalNew;
	private File myCurDir;
	private File myNewDir;
	private String[] myReadMessages;
	private String[] myUnreadMessages;
	
	public ImapMailbox(String label, String filename) throws IOException {
		this.myLabel = label;
		this.myCurDir = new File(filename + "cur" + File.separator);
		this.myNewDir = new File(filename + "new" + File.separator);
		if(!(this.myCurDir.exists()) || !(this.myNewDir.exists()))
			throw new IOException();
	}
	
	private void setTotalVars() throws IOException {
		this.myReadMessages = this.myCurDir.list();
		this.myUnreadMessages = this.myNewDir.list();
		this.myTotalNum = this.myReadMessages.length + this.myUnreadMessages.length;
		this.myTotalNew = this.myUnreadMessages.length;
	}
	
	public int getNumNew() {	return this.myTotalNew;	}
	
	public int getNumTotal() {	return this.myTotalNum;	}
	
	public boolean hasNew() {
		if(this.myTotalNew > 0)
			return true;
		return false;
	}
	
	public String getLabel() {	return this.myLabel;	}
}
