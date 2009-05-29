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

/**
 * @author rmanocha
 *
 */
public class ReadOptsException extends Exception{

	private static final long serialVersionUID = 8340367885814675529L;		//I have no idea what this is all about.It just stops giving me errors in eclipse.

	public ReadOptsException() {	super();	}
		
	public ReadOptsException(String errorMsg) {	super(errorMsg);	}
}
