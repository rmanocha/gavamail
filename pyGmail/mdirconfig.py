#!/usr/bin/python
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Library General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.

import gtk, gtk.glade, os, string, ConfigParser, sys
	
def print_syntax():
	syntax = "mdirconfig.py 0.2\nUSAGE: mdirconfig.py <confile>\nconfile\t\tThe PyMdirGavaMail configuration file"
	return syntax

class MyWindow:
	def __init__(self, args, glade_path="pygmail.glade"):
		try:
			self.confFile = args[1]
		except IndexError:
			sys.stderr.write("You did not specify a configuration filename.\n%s\n" % print_syntax())
			sys.exit(2)
		self.widgets = gtk.glade.XML(glade_path)
		self.myWritten = ConfigParser.ConfigParser()
		self.myWritten.read(self.confFile)
		try:
			self.username = self.myWritten.get("DEFAULT","username")
			self.password = self.myWritten.get("DEFAULT","password")
			self.mdir = self.myWritten.get("DEFAULT","baseMdir")
			self.label = self.myWritten.get("DEFAULT","label")
			self.unreadonly = self.myWritten.getboolean("DEFAULT","unread-only")
			self.usemysql = self.myWritten.getboolean("DEFAULT","use-mysql")
			self.mysql_user_text = self.myWritten.get("DEFAULT","mysql-username")
			self.mysql_pass_text = self.myWritten.get("DEFAULT","mysql-password")
			self.mysql_db_text = self.myWritten.get("DEFAULT","mysql-database")
			self.mysql_host_text = self.myWritten.get("DEFAULT","mysql-host")
		except ConfigParser.NoOptionError:
			sys.stderr.write("The file you provided is not a mdirgmail.py configuration file.\n%s\n" % print_syntax())
			sys.exit(1)
		except ValueError:
			self.unreadonly = False
		self.widgets.signal_autoconnect(self)
		
		#set text for the username
		self.userEntry = self.widgets.get_widget("userEntry")
		self.userEntry.set_text(self.username)
		
		#set text for password
		self.passEntry = self.widgets.get_widget("passEntry")
		self.passEntry.set_text(self.password)
		
		#set folder to select
		self.mdirDialog = self.widgets.get_widget("selectMdir")
		self.mdirDialog.set_current_folder(os.path.abspath(self.mdir))
		
		#set text for labels
		self.labelsEntry = self.widgets.get_widget("labelsEntry")
		self.labelsEntry.set_text(self.label)
		
		#set the tick mark for unread-only
		self.unreadOnlyButton = self.widgets.get_widget("unreadOnlyButton")
		self.unreadOnlyButton.set_active(self.unreadonly)
		
		#set the tick mark for mysql button and accordingly activate/deavtivate rest of mysql options
		self.useMysqlButton = self.widgets.get_widget("usemysql")
		self.useMysqlButton.set_active(self.usemysql)
		
		self.mysqlUser = self.widgets.get_widget("mysql_user")
		self.mysqlUser.set_text(self.mysql_user_text)
		
		self.mysqlPass = self.widgets.get_widget("mysql_pass")
		self.mysqlPass.set_text(self.mysql_pass_text)
		
		self.mysqlDb = self.widgets.get_widget("mysql_db")
		self.mysqlDb.set_text(self.mysql_db_text)
		
		self.mysqlHost = self.widgets.get_widget("mysql_host")
		self.mysqlHost.set_text(self.mysql_host_text)
		
		if self.usemysql:
			self.mysqlUser.set_editable(True)
			self.mysqlPass.set_editable(True)
			self.mysqlDb.set_editable(True)
			self.mysqlHost.set_editable(True)
		
		#get the second window.
		self.myWindow = self.widgets.get_widget("myWindow")
		self.askSaveWindow = self.widgets.get_widget("askSave")
		self.askSaveWindow.set_transient_for(self.myWindow)
		self.selectMdir = self.widgets.get_widget("selectMdir")
		self.selectMdir.set_transient_for(self.myWindow)
		
		self.needSave = False
		
	def on_save1_activate(self, widget, *args):
		self.myWritten.set("DEFAULT","username",self.userEntry.get_text())
		self.myWritten.set("DEFAULT","password",self.passEntry.get_text())
		self.myWritten.set("DEFAULT","baseMdir",self.mdirDialog.get_current_folder() + os.sep)
		self.myWritten.set("DEFAULT","label",self.labelsEntry.get_text())
		self.myWritten.set("DEFAULT","unread-only",str(self.unreadOnlyButton.get_active()))
		self.myWritten.set("DEFAULT","use-mysql",str(self.useMysqlButton.get_active()))
		self.myWritten.set("DEFAULT","mysql-username",self.mysqlUser.get_text())
		self.myWritten.set("DEFAULT","mysql-password",self.mysqlPass.get_text())
		self.myWritten.set("DEFAULT","mysql-database",self.mysqlDb.get_text())
		self.myWritten.set("DEFAULT","mysql-host",self.mysqlHost.get_text())
		self.myWritten.write(open(self.confFile,"w"))
		self.needSave = False
	
	def on_usemysql_toggled(self, widget, *args):
		self.needSave = True
		if self.useMysqlButton.get_active():
			self.mysqlUser.set_editable(True)
			self.mysqlPass.set_editable(True)
			self.mysqlDb.set_editable(True)
			self.mysqlHost.set_editable(True)
		else:
			self.mysqlUser.set_editable(False)
			self.mysqlPass.set_editable(False)
			self.mysqlDb.set_editable(False)
			self.mysqlHost.set_editable(False)
	
	def on_quit1_activate(self, widget, *args):
		if self.needSave:
			self.askSaveWindow.show_all()
		else:
			gtk.main_quit()
	
	def on_mdirBrowseButton_clicked(self, widget, *args):
		self.selectMdir.show_all()
	
	def on_selectMdirCancel_clicked(self, widget, *args):
		self.selectMdir.hide()
		
	def on_selectMdirOpen_clicked(self, widget, *args):
		self.selectMdir.hide()
	
	def on_saveQuit_clicked(self, widget, *args):
		self.on_save1_activate(widget, args)
		gtk.main_quit()
	
	def on_noSave_clicked(self, widget, *args):
		gtk.main_quit()
		
	def on_cancelQuit_clicked(self, widget, *args):
		self.askSaveWindow.hide()
		
	def on_about1_activate(self, widget, *args):
		"""
		Show the about window
		"""
		aboutWin = gtk.AboutDialog()
		aboutWin.set_name("About - MdirConfig")
		aboutWin.set_version("0.2")
		aboutWin.set_license("Governed by the GPL v.2, see COPYING for details")
		aboutWin.set_copyright("(c) Rishabh Manocha 2004-2005")
		aboutWin.set_comments("A configuration editor for PyMdirGavaMail written in Python and PyGTK.")
		aboutWin.set_website("http://gavamail.sourceforge.net/phpwiki/")
		aboutWin.set_website_label("Website")
		aboutWin.set_authors("Rishabh Manocha")
		aboutWin.set_documenters("Rishabh Manocha")
		#aboutWin.set_logo("logo.png")
		aboutWin.show_all()
	
	def on_userEntry_changed(self, widget, *args):
		self.needSave = True
	
	def on_passEntry_changed(self, widget, *args):
		self.needSave = True
	
	def on_selectMdir_current_folder_changed(self, widget, *args):
		self.needSave = True
	
	def on_labelsEntry_changed(self, widget, *args):
		self.needSave = True
	
	def on_unreadOnlyButton_toggled(self, widget, *args):
		self.needSave = True
	
	def on_myWindow_destroy(self, widget, *args):
		if self.needSave:
			self.askSaveWindow.show_all()
		else:
			gtk.main_quit()
		
if __name__ == "__main__":
	window = MyWindow(sys.argv)
	gtk.main()
