package me.skymc.flowerpot;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.FlowerPot;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import me.skymc.taboolib.inventory.ItemUtils;
import me.skymc.taboolib.location.LocationUtils;
import me.skymc.taboolib.other.NumberUtils;
import me.skymc.taboolib.particle.EffLib;
import me.skymc.taboolib.playerdata.DataUtils;

/**
 * @author sky
 * @since 2018年2月19日 下午2:16:24
 */
public class FlowerPotBreaker extends JavaPlugin implements Listener {
	
	private FileConfiguration data;
	
	@Override
	public void onLoad() {
		saveDefaultConfig();
	}
	
	@Override
	public void onEnable() {
		// 注册配置
		data = DataUtils.addPluginData("data.yml", this);
		// 注册监听
		Bukkit.getPluginManager().registerEvents(this, this);
		
		// 恢复任务
		new BukkitRunnable() {
			
			@Override
			public void run() {
				// 恢复
				reset(false);
			}
		}.runTaskTimer(this, 0, 40);
	}
	
	@Override
	public void onDisable() {
		Bukkit.getScheduler().cancelTasks(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage("§f/flowerpot reload §6- §e重载配置");
			sender.sendMessage("§f/flowerpot update §6- §e重载花盆");
		}
		else if (args[0].equalsIgnoreCase("reload")) {
			reloadConfig();
			sender.sendMessage("reload ok!");
		}
		else if (args[0].equalsIgnoreCase("update")) {
			long time = System.currentTimeMillis();
			reset(true);
			sender.sendMessage("§7更新完成, 耗时: §7" + (System.currentTimeMillis() - time) + "ms");
		}
		return true;
	}
	
	@EventHandler (priority = EventPriority.LOWEST)
	public void onBreak(BlockBreakEvent e) {
		// 检测破坏方块以及是否在禁用世界内以及生存模式
		if (e.getBlock().getState() instanceof FlowerPot && !getConfig().getStringList("DisableWorld").contains(e.getPlayer().getWorld().getName()) && e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
			// 取消事件
			e.setCancelled(true);
			
			// 获取数据
			FlowerPot flowerPot = (FlowerPot) e.getBlock().getState();
			// 序列化坐标
			String location = LocationUtils.fromString(e.getBlock().getLocation()).replace(".", "_");
			
			// 设置破坏时间
			data.set(location + ".break", System.currentTimeMillis());
			// 花盆内是否有内容
			if (flowerPot.getContents() != null && !flowerPot.getContents().getItemType().equals(Material.AIR)) {
				// 获取物品
				ItemStack contents = flowerPot.getContents().toItemStack();
				// 设置花盆内容
				data.set(location + ".content", contents.getType().toString() + ":" + contents.getDurability());
			}

			// 删除花盆内容
			flowerPot.setContents(new MaterialData(Material.AIR));
			flowerPot.update(false, false);
			
			// 删除花盆
			e.getBlock().setType(Material.AIR);
			
			// 掉落物品
			for (String dropStr : getConfig().getStringList("Drops")) {
				// 几率
				if (NumberUtils.getRand().nextDouble() < Double.valueOf(dropStr.split(" ")[0])) {
					// 生成物品
					ItemStack drop = ItemUtils.getCacheItem(dropStr.split(" ")[1]);
					// 如果物品存在
					if (drop != null) {
						e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0, 0.5), drop);
					}
				}
			}
		}
	}
	

	/**
	 * 更新花盆
	 * 
	 * @param force 强制?
	 */
	@SuppressWarnings("deprecation")
	public void reset(boolean force) {
		for (String locationStr : data.getConfigurationSection("").getKeys(false)) {
			// 是否可以更新
			if (force || System.currentTimeMillis() - data.getLong(locationStr + ".break") >= getConfig().getInt("UpdateDelay") * 1000) {
				// 获取坐标
				Block block = LocationUtils.toString(locationStr.replace("_", ".")).getBlock();
				// 设置方块
				block.setType(Material.FLOWER_POT);
				// 如果有内容
				if (data.contains(locationStr + ".content")) {
					FlowerPot flowerPot = (FlowerPot) block.getState();
					
					// 获取内容
					Material material = ItemUtils.asMaterial(data.getString(locationStr + ".content").split(":")[0]);
					byte _byte = Byte.valueOf(data.getString(locationStr + ".content").split(":")[1]);
					
					// 设置内容
					flowerPot.setContents(new MaterialData(material, _byte));
					flowerPot.update(false, false);
				}
				// 删除数据
				data.set(locationStr, null);
				// 播放粒子
				EffLib.CLOUD.display(0.5f, 0.5f, 0.5f, 0, 10, block.getLocation().add(0.5, 0, 0.5), 20);
			}
		}
	}
}
