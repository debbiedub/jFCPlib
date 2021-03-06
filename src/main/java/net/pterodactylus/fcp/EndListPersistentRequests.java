/*
 * jFCPlib - EndListPersistentRequests.java - Copyright © 2008–2016 David Roden
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.pterodactylus.fcp;

/**
 * The “EndListPersistentRequests” message signals the end of a list of
 * {@link PersistentGet} and {@link PersistentPut} requests.
 *
 * @author David ‘Bombe’ Roden &lt;bombe@freenetproject.org&gt;
 */
public class EndListPersistentRequests extends BaseMessage {

	/**
	 * Creates a new “EndListPersistentRequests” message that wraps the
	 * received message.
	 *
	 * @param receivedMessage
	 *            The received message
	 */
	public EndListPersistentRequests(FcpMessage receivedMessage) {
		super(receivedMessage);
	}

}
