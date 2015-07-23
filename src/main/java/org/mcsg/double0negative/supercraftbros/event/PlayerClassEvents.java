package org.mcsg.double0negative.supercraftbros.event;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.mcsg.double0negative.supercraftbros.Game;
import org.mcsg.double0negative.supercraftbros.GameManager;
import org.mcsg.double0negative.supercraftbros.SettingsManager;
import org.mcsg.double0negative.supercraftbros.Game.State;

import net.minecraft.server.v1_8_R3.Packet;

public class PlayerClassEvents implements Listener{

	GameManager gm;
	
	protected boolean smash = false;

	private boolean doublej = false;
	protected boolean fsmash  = false;


	public PlayerClassEvents(){
		gm = GameManager.getInstance();
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void blockFire(BlockIgniteEvent e){
		final Block b = e.getBlock();
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				b.setTypeId(0);
				b.getState().update();
			}
		}, 60);
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent e){
		Player p = e.getPlayer();

		int id = gm.getPlayerGameId(p);
		if(id != -1){
			Game g = gm.getGame(id);
			if(g.getState() == Game.State.INGAME){
				if(e.getPlayer().getItemInHand().getType() == Material.DIAMOND_AXE){
					Smash(p);
				}
				else if(p.getItemInHand().getType() == Material.EYE_OF_ENDER){
					e.setCancelled(true);
				}
				else{
				//	g.getPlayerClass(p).PlayerInteract(e.getAction());
				}
			}
		}

	}
	
	public void Smash(Player p){
		
	}

	@SuppressWarnings("deprecation")
	public boolean isOnGround(Player p){
		Location l = p.getLocation();
		l = l.add(0, -1, 0);
		return l.getBlock().getState().getTypeId() != 0;
	}

	public void explodePlayers(Player p){
		int i = GameManager.getInstance().getPlayerGameId(p);
		if(i != -1){
			Set<Player>pls = GameManager.getInstance().getGame(i).getActivePlayers();

			Location l = p.getLocation();
			l = l.add(0, -1, 0);
			for(int x = l.getBlockX() - 1; x<=l.getBlockX()+1; x++){
				for(int z = l.getBlockZ() - 1; z<=l.getBlockZ()+1; z++){
				 //SendPacketToAll(new PacketPlayOutWorldEvent(2001,x, l.getBlockY()+1, z, l.getBlock().getState().getTypeId(), false));
					explodeBlocks(p, new Location(l.getWorld(), x, l.getBlockY(), z));
				}
			}
			for(Entity pl:p.getWorld().getEntities()){
				if(pl != p){
					ItemStack s = p.getItemInHand();
					Location l2 = pl.getLocation();
					double d = pl.getLocation().distance(p.getLocation());
					if(d < 5){
						d = d / 5;
						pl.setVelocity(new Vector((1.5-d) * getSide(l2.getBlockX(), l.getBlockX()), 1.5-d, (1.5-d)*getSide(l2.getBlockZ(), l.getBlockZ())));
						
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void explodeBlocks(Player p, Location l){
		Location l2 = p.getLocation();
		if(l.getBlock().getState().getTypeId() != 0){
			final Entity e  = l.getWorld().spawnFallingBlock(l, l.getBlock().getState().getTypeId(), l.getBlock().getState().getData().getData());
			e.setVelocity(new Vector((getSide(l.getBlockX(), l2.getBlockX()) * 0.3),.1, (getSide(l.getBlockZ(), l2.getBlockZ()) * 0.3)));
			Bukkit.getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), new Runnable(){
				public void run(){
					e.remove();
				}
			}, 5);
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void SendPacketToAll(Packet p, Player player){
		for(Player pl: GameManager.getInstance().getGame(GameManager.getInstance().getPlayerGameId(player)).getActivePlayers()){
			((CraftPlayer)pl).getHandle().playerConnection.sendPacket(p);
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent e){
		Player p = e.getPlayer();
		int id = gm.getPlayerGameId(p);
		if(id != -1){
			Game g = gm.getGame(id);
			if(g.getState() == Game.State.INGAME){
				if(p.isFlying()){
					p.setFlying(false);
					p.setAllowFlight(false);
					Vector v = p.getLocation().getDirection().multiply(.5);
					v.setY(.75);
					p.setVelocity(v);
					doublej = true;
				}
				if(isOnGround(p)){
					p.setAllowFlight(true);
					if(fsmash){
						explodePlayers(p);
						fsmash = false;
					}
					doublej = false;

				}
				if(doublej && p.isSneaking()){
					p.setVelocity(new Vector(0, -1, 0));
					fsmash = true;
				}
			}	
		}
	}

	public int getSide(int i, int u){
		if(i > u) return 1;
		if(i < u)return -1;
		return 0;
	}

	@EventHandler
	public void onEntityDamaged(EntityDamageEvent e){
		if(e.getEntity() instanceof Player){
			Player p = (Player)e.getEntity();
			int game = GameManager.getInstance().getPlayerGameId(p);
			if(game != -1){
				Game g = gm.getGame(game);
				if(g.getState() == Game.State.INGAME){
					if(smash){
						p.setHealth(20);
					}
				}
			}
		}
	}

	@EventHandler
	public void onEntityDamaged(EntityDamageByEntityEvent e){
		try{
			Player victim = null;
			Player attacker = null;
			if(e.getEntity() instanceof Player){
				victim = (Player)e.getEntity();
			}
			if(e.getDamager() instanceof Player){
				attacker = (Player)e.getDamager();
			}
			if(victim != null && attacker != null){
				if(gm.getPlayerGameId(victim) != -1 && gm.getPlayerGameId(attacker) != -1){
					if(smash){
						victim.setHealth(20);
					}
				//	gm.getPlayerClass(attacker).PlayerAttack(victim);
				}
			}
		}catch(Exception et){}

	}


	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent e){
		e.blockList().clear();
		Location l = e.getLocation();
		if(e.getEntity() instanceof Fireball){
			e.setCancelled(true);
			double x = l.getX();
			double y = l.getY();
			double z = l.getZ();
			l.getWorld().createExplosion(x, y, z, 3, false, false);
		}
	}

	@EventHandler
	public void onEntityDeath(PlayerDeathEvent e){
		if(e.getEntity() instanceof Player){
			Player p = (Player)e.getEntity();

			int id = gm.getPlayerGameId(p);
			if(id != -1){
			//	gm.getPlayerClass(p).PlayerDeath();
			}
		}
	}

	@EventHandler
	public void onEntityDeath(EntityShootBowEvent e){
		if(e.getEntity() instanceof Player){
			Player p = (Player)e.getEntity();
			int game = GameManager.getInstance().getPlayerGameId(p);
			if(game != -1){
			//	gm.getGame(game).getPlayerClass(p).PlayerShootArrow(e.getProjectile());
			}

		}
	}


	@EventHandler
	public void onEntityRespawn(PlayerRespawnEvent e){
		final Player p = e.getPlayer();
		Bukkit.getScheduler().scheduleSyncDelayedTask(GameManager.getInstance().getPlugin(), new Runnable(){
			public void run(){
				int id = gm.getPlayerGameId(p);
				if(id != -1){
					gm.getGame(id).spawnPlayer(p);
				//	gm.getPlayerClass(p).PlayerSpawn();
				}
				else{
					p.teleport(SettingsManager.getInstance().getLobbySpawn());
				}
			}
		}, 1);
	}

	@EventHandler
	public void onPlayerPlaceBlock(BlockPlaceEvent e){
		int id = gm.getPlayerGameId(e.getPlayer());
		if(id != -1){
			if(gm.getGame(id).getState() == State.INGAME){
			//	gm.getPlayerClass(e.getPlayer()).PlayerPlaceBlock(e.getBlock());
		}
	}
}
}	
