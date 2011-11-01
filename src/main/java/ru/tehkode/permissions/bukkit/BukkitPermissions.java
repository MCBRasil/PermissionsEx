/*
 * PermissionsEx - Permissions plugin for Bukkit
 * Copyright (C) 2011 t3hk0d3 http://www.tehkode.ru
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package ru.tehkode.permissions.bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.ConfigurationNode;
import ru.tehkode.permissions.bukkit.superperms.PermissiblePEX;

public class BukkitPermissions {

	protected static final Logger logger = Logger.getLogger("Minecraft");
	protected Map<Player, PermissionAttachment> attachments = new HashMap<Player, PermissionAttachment>();
	protected Plugin plugin;
	protected boolean strictMode = false;
	protected boolean enableParentNodes = true;
	protected Map<String, Map<String, Boolean>> childPermissions = new HashMap<String, Map<String, Boolean>>();

	public BukkitPermissions(Plugin plugin, ConfigurationNode config) {
		this.plugin = plugin;

		if (!config.getBoolean("enable", true)) {
			logger.info("[PermissionsEx] Superperms disabled. Check \"config.yml\" to enable.");
			return;
		}

		this.strictMode = config.getBoolean("strict-mode", strictMode);
		this.enableParentNodes = config.getBoolean("parent-nodes", this.enableParentNodes);

		this.registerEvents();

		if (this.enableParentNodes) {
			this.calculateParentPermissions();
		}

		logger.info("[PermissionsEx] Superperms support enabled.");
	}

	public Map<String, Map<String, Boolean>> getChildPermissions() {
		return childPermissions;
	}

	public boolean isStrictMode() {
		return strictMode;
	}

	public boolean isEnableParentNodes() {
		return enableParentNodes;
	}

	public Plugin getPlugin() {
		return plugin;
	}

	protected void calculateParentPermissions() {
		for (Permission permission : this.plugin.getServer().getPluginManager().getPermissions()) {
			for (Map.Entry<String, Boolean> child : permission.getChildren().entrySet()) {
				Map<String, Boolean> map = this.childPermissions.get(child.getKey().toLowerCase());
				if (map == null) {
					this.childPermissions.put(child.getKey().toLowerCase(), map = new HashMap<String, Boolean>());
				}

				map.put(permission.getName(), child.getValue());
			}
		}
	}

	private void registerEvents() {
		PluginManager manager = plugin.getServer().getPluginManager();

		manager.registerEvent(Event.Type.PLAYER_JOIN, new PlayerEvents(), Event.Priority.Low, plugin);

		if (this.enableParentNodes) {
			ServerListener serverEvents = new ServerEvents();

			manager.registerEvent(Event.Type.PLUGIN_ENABLE, serverEvents, Event.Priority.Low, plugin);
			manager.registerEvent(Event.Type.PLUGIN_DISABLE, serverEvents, Event.Priority.Low, plugin);
		}
	}

	public void updatePermissions(Player player) {
		this.updatePermissions(player, null);
	}

	public void updatePermissions(Player player, String world) {
		if (player == null || !this.plugin.isEnabled()) {
			return;
		}

		PermissiblePEX.inject(player, this);
	}

	public void updateAllPlayers() {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			updatePermissions(player);
		}
	}

	protected class PlayerEvents extends PlayerListener {

		@Override
		public void onPlayerJoin(PlayerJoinEvent event) {
			updatePermissions(event.getPlayer());
		}
	}

	protected class ServerEvents extends ServerListener {

		@Override
		public void onPluginDisable(PluginDisableEvent event) {
			calculateParentPermissions();
		}

		@Override
		public void onPluginEnable(PluginEnableEvent event) {
			calculateParentPermissions();
		}
	}
}
