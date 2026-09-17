package handler;



import inout.Protocol;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Hashtable;

import server.ClientHandler;
import server.Server;
import Packet.CommandPacket;
import Packet.LogPacket;
import Packet.Packet;

public class CommandHandler implements PacketHandler
{
	private short command;
	private byte[] arg;

	public CommandHandler()
	{
		
	}
	
	@Override
	public void handlePacket(Packet p,String temp_imei,Server c) 
	{
		  

			command = ((CommandPacket) p).getCommand();
			arg = ((CommandPacket) p).getArguments();
			
			switch (command) 
			{
				case Protocol.CONNECT:
					
					// Reconstruction des infos
					
					ByteArrayInputStream bis = new ByteArrayInputStream(arg);
					ObjectInputStream in;
					Hashtable<String,String> h = null;
					try {
						in = new ObjectInputStream(bis);
						h = (Hashtable<String, String>) in.readObject();
					} catch (Exception e) {
						e.printStackTrace();
					}
					String new_imei = h.get("IMEI");
					
					c.getGui().logTxt("CONNECT command received from "+new_imei);
					//dans le cas d'un tout nouveau imei 
					if(!c.getClientMap().containsKey(new_imei))
					{

						//on r�cup�re son gestionnaire
						ClientHandler ch = c.getClientMap().get(temp_imei);
						ChannelDistributionHandler cdh = c.getChannelHandlerMap().get(temp_imei);
						
						ch.updateIMEI(new_imei); //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
						
						//on efface ses donn�es tomporaires attribu�es � la connexion
						c.getClientMap().remove(temp_imei);
						c.getChannelHandlerMap().remove(temp_imei);
						
						
						//et on l'inscrit
						c.getClientMap().put(new_imei, ch);
						c.getChannelHandlerMap().put(new_imei,cdh);
						
						// Generate and send session token for new device registration
						String sessionToken = c.generateSessionToken(new_imei);
						c.getGui().logTxt("New device registered with IMEI: " + new_imei);
						// TODO: Send session token back to client for future reconnections
						// This would require protocol modification to send the token to the client
						
						//On ajoute le handler pour les logs
						c.getChannelHandlerMap().get(new_imei).registerListener(1, new LogPacket());
						c.getChannelHandlerMap().get(new_imei).registerHandler(1, new ClientLogHandler(1, new_imei, c.getGui()));
					}
					//si le client s'est reconnect� (imei d�ja inscrit)
					else
					{
						// SECURITY: Validate session token before allowing reconnection
						String providedToken = h.get("SessionToken");
						
						if (providedToken == null || !c.validateSessionToken(new_imei, providedToken)) {
							// Authentication failed - reject the connection attempt
							c.getGui().logErrTxt("SECURITY ALERT: Unauthorized reconnection attempt for IMEI: " + new_imei + 
								" from temporary ID: " + temp_imei + ". Connection rejected.");
							
							// Close the unauthorized connection
							ClientHandler unauthorizedCh = c.getClientMap().get(temp_imei);
							if (unauthorizedCh != null) {
								// Remove temporary entries without affecting the legitimate device
								c.getClientMap().remove(temp_imei);
								c.getChannelHandlerMap().remove(temp_imei);
								// The ClientHandler thread will detect disconnection and clean up
							}
							
							// Do NOT proceed with reconnection - return early
							break;
						}
						
						// Token validated - proceed with legitimate reconnection
						c.getGui().logTxt("Authenticated reconnection for IMEI: " + new_imei);
						
						//on r�cup�re son gestionnaire
						ClientHandler ch1 = c.getClientMap().get(temp_imei);
						//et son ANCIEN ChannelDistributionHandler!
						ChannelDistributionHandler cdh1 = c.getChannelHandlerMap().get(new_imei);
						
						//on efface ses donn�es tomporaires attribu�es � la connexion 
						c.getClientMap().remove(temp_imei);
						c.getChannelHandlerMap().remove(temp_imei);
						//et lors de la connexion pr�c�dente!
						c.getChannelHandlerMap().remove(new_imei);
						
						//et on l'inscrit avec son ancien ChannelDistributoinHandler
						c.getClientMap().put(new_imei, ch1);
						c.getChannelHandlerMap().put(new_imei,cdh1);
	
						
					}
					c.getGui().addUser(new_imei, h.get("Country"), h.get("PhoneNumber"), h.get("Operator"), h.get("SimCountry"), h.get("SimOperator"), h.get("SimSerial"));

					break;

			}
		
		
	}

	@Override
	public void receive(Packet p, String imei) {
		// TODO Auto-generated method stub
		
	}


}
