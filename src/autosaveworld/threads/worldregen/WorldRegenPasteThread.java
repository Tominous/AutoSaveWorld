/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
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
 *
 */

package autosaveworld.threads.worldregen;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import autosaveworld.config.AutoSaveWorldConfig;
import autosaveworld.config.AutoSaveWorldConfigMSG;
import autosaveworld.core.AutoSaveWorld;
import autosaveworld.core.GlobalConstants;
import autosaveworld.core.logging.MessageLogger;
import autosaveworld.threads.worldregen.factions.FactionsPaste;
import autosaveworld.threads.worldregen.griefprevention.GPPaste;
import autosaveworld.threads.worldregen.pstones.PStonesPaste;
import autosaveworld.threads.worldregen.towny.TownyPaste;
import autosaveworld.threads.worldregen.wg.WorldGuardPaste;
import autosaveworld.utils.FileUtils;
import autosaveworld.utils.ListenerUtils;
import autosaveworld.utils.SchedulerUtils;

public class WorldRegenPasteThread extends Thread {

	private AutoSaveWorld plugin = null;
	private AutoSaveWorldConfig config;
	private AutoSaveWorldConfigMSG configmsg;

	public WorldRegenPasteThread(AutoSaveWorld plugin, AutoSaveWorldConfig config, AutoSaveWorldConfigMSG configmsg) {
		this.plugin = plugin;
		this.config = config;
		this.configmsg = configmsg;
	};

	public boolean shouldPaste() {
		File check = new File(GlobalConstants.getWorldnameFile());
		if (check.exists()) {
			return true;
		}
		return false;
	}

	@Override
	public void run() {

		Thread.currentThread().setName("AutoSaveWorld WorldRegenPaste Thread");

		try {
			doWorldPaste();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void doWorldPaste() throws InterruptedException {
		// deny players from join
		ListenerUtils.registerListener(new AntiJoinListener(configmsg));

		// load config
		FileConfiguration cfg = YamlConfiguration.loadConfiguration(new File(GlobalConstants.getWorldnameFile()));
		String worldtopasteto = cfg.getString("wname");

		// wait for server to start
		SchedulerUtils.callSyncTaskAndWait(new Runnable() {
			@Override
			public void run() {
			}
		});

		// check for worldedit
		if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
			MessageLogger.broadcast("WorldEdit not found, can't place schematics back, please install WorldEdit and restart server", true);
			return;
		}

		MessageLogger.debug("Restoring buildings");

		// paste WG buildings
		if ((Bukkit.getPluginManager().getPlugin("WorldGuard") != null) && config.worldRegenSaveWG) {
			new WorldGuardPaste(worldtopasteto).pasteAllFromSchematics();
		}

		// paste Factions buildings
		if ((Bukkit.getPluginManager().getPlugin("Factions") != null) && config.worldRegenSaveFactions) {
			new FactionsPaste(worldtopasteto).pasteAllFromSchematics();
		}

		// paste GriefPrevention claims
		if ((Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) && config.worldRegenSaveGP) {
			new GPPaste(worldtopasteto).pasteAllFromSchematics();
		}

		// paste Towny towns
		if ((Bukkit.getPluginManager().getPlugin("Towny") != null) && config.worldregenSaveTowny) {
			new TownyPaste(worldtopasteto).pasteAllFromSchematics();
		}

		// paste PStones regions
		if ((Bukkit.getPluginManager().getPlugin("PreciousStones") != null) && config.worldregenSavePStones) {
			new PStonesPaste(worldtopasteto).pasteAllFromSchematics();
		}

		// clear temp folder
		MessageLogger.debug("Cleaning temp folders");

		FileUtils.deleteDirectory(new File(GlobalConstants.getWGTempFolder()));
		FileUtils.deleteDirectory(new File(GlobalConstants.getFactionsTempFolder()));
		FileUtils.deleteDirectory(new File(GlobalConstants.getGPTempFolder()));
		FileUtils.deleteDirectory(new File(GlobalConstants.getTownyTempFolder()));
		FileUtils.deleteDirectory(new File(GlobalConstants.getPStonesTempFolder()));
		new File(GlobalConstants.getWorldnameFile()).delete();
		new File(GlobalConstants.getWorldRegenTempFolder()).delete();

		MessageLogger.debug("Restore finished");

		// save server, just in case
		MessageLogger.debug("Saving server");
		plugin.saveThread.performSave();

		// restart
		MessageLogger.debug("Restarting server");
		plugin.autorestartThread.startrestart(true);
	}

}
