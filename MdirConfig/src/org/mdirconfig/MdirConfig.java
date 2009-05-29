/*
 *	Created on Nov 29, 2004
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
package org.mdirconfig;

import org.gnu.glade.LibGlade;
import org.gnu.gtk.*;
import org.gnu.gnome.About;
import org.gnu.gdk.Pixbuf;

import java.util.Properties;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * @author rmanocha
 *
 */
public class MdirConfig {
	private transient LibGlade myGlade;
	private transient Properties myPropsFile;
	private transient Entry myUsername;
	private transient Entry myPassword;
	private transient Entry myMdir;
	private transient Entry myLabels;
	private transient CheckButton myUnread;
	private transient boolean needSave;
	private transient String myConfFile;
	private transient Window myAskSaveWindow;

	public MdirConfig(String glade_path, String propsfile) {
		this.myConfFile = propsfile;
		this.myPropsFile = new Properties();
		try {
			this.myPropsFile.load(new FileInputStream(this.myConfFile));
			this.myGlade = new LibGlade(glade_path,this);
		}catch (Exception e) {	e.printStackTrace();	}
		
		//load the various entries.
		//the username
		this.myUsername = (Entry)this.myGlade.getWidget("userEntry");
		this.myUsername.setText(this.myPropsFile.getProperty("username"));
		
		//the password
		this.myPassword = (Entry)this.myGlade.getWidget("passEntry");
		this.myPassword.setText(this.myPropsFile.getProperty("password"));
		
		//base maildir directory
		this.myMdir = (Entry)this.myGlade.getWidget("mdirEntry");
		this.myMdir.setText(this.myPropsFile.getProperty("baseMdir"));
		
		//all labels
		this.myLabels = (Entry)this.myGlade.getWidget("labelsEntry");
		this.myMdir.setText(this.myPropsFile.getProperty("label"));
		
		//unread only button
		this.myUnread = (CheckButton)this.myGlade.getWidget("unreadOnlyButton");
		String tmp = this.myPropsFile.getProperty("unread-only");
		if(tmp.equalsIgnoreCase("true"))
			this.myUnread.setState(true);
		else
			this.myUnread.setState(false);
		
		//get the ask_save window
		this.myAskSaveWindow = (Window)this.myGlade.getWidget("askSave");
		
	}
	
	public void on_save1_activate() {
		this.myPropsFile.setProperty("username",this.myUsername.getText());
		this.myPropsFile.setProperty("password",this.myPassword.getText());
		this.myPropsFile.setProperty("baseMdir",this.myMdir.getText());
		this.myPropsFile.setProperty("label",this.myLabels.getText());
		this.myPropsFile.setProperty("unread-only", Boolean.toString(this.myUnread.getState()));
		try {
			this.myPropsFile.store(new FileOutputStream(this.myConfFile),"");
		} catch (Exception e) {	e.printStackTrace();	}
	}
	
	public void on_quit1_activate() {
		if (this.needSave)
			this.myAskSaveWindow.showAll();
		else
			Gtk.mainQuit();
	}
	
	public void on_saveQuit_clicked() {
		this.on_save1_activate();
		Gtk.mainQuit();
	}
	
	public void on_noSave_clicked() {
		Gtk.mainQuit();
	}
	
	public void on_cancelQuit_clicked() {
		this.myAskSaveWindow.hide();
	}
	
	public void on_about1_activate() {
		String[] authors = {"Rishabh Manocha<rmanocha@gmail.com>"};
		String[] documentors = {"Rishabh Manocha<rmanocha@gmail.com>"};
		About aboutWindow = new About("About - MdirConfig", "0.1","GPL v2.0","MdirConfig is a grpahical utility to edit MdirGavaMail configuration files.",authors,documentors,"",new Pixbuf("logo.png"));
		aboutWindow.showAll();
	}
	
	public void on_userEntry_changed() {	this.needSave = true;	}
	
	public void on_passEntry_changed() {	this.needSave = true;	}
	
	public void on_mdirEntry_changed() {	this.needSave = true;	}
	
	public void on_labelsEntry_changed() {	this.needSave = true;	}
	
	public void on_unreadOnlyButton_toggled() {	this.needSave = true;	}
	
	public void on_myWindow_destroy() {
		this.on_quit1_activate();
	}
	
	public static void main(String[] args) {
		MdirConfig conf = new MdirConfig("pygmail.glade",".mdirgmailrc");
		Gtk.init(args);
		Gtk.main();
	}
}
