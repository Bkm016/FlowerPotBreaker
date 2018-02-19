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
 * @since 2018��2��19�� ����2:16:24
 */
public class FlowerPotBreaker extends JavaPlugin implements Listener {
	
	private FileConfiguration data;
	
	@Override
	public void onLoad() {
		saveDefaultConfig();
	}
	
	@Override
	public void onEnable() {
		// ע������
		data = DataUtils.addPluginData("data.yml", this);
		// ע�����
		Bukkit.getPluginManager().registerEvents(this, this);
		
		// �ָ�����
		new BukkitRunnable() {
			
			@Override
			public void run() {
				// �ָ�
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
			sender.sendMessage("��f/flowerpot reload ��6- ��e��������");
			sender.sendMessage("��f/flowerpot update ��6- ��e���ػ���");
		}
		else if (args[0].equalsIgnoreCase("reload")) {
			reloadConfig();
			sender.sendMessage("reload ok!");
		}
		else if (args[0].equalsIgnoreCase("update")) {
			long time = System.currentTimeMillis();
			reset(true);
			sender.sendMessage("��7�������, ��ʱ: ��7" + (System.currentTimeMillis() - time) + "ms");
		}
		return true;
	}
	
	@EventHandler (priority = EventPriority.LOWEST)
	public void onBreak(BlockBreakEvent e) {
		// ����ƻ������Լ��Ƿ��ڽ����������Լ�����ģʽ
		if (e.getBlock().getState() instanceof FlowerPot && !getConfig().getStringList("DisableWorld").contains(e.getPlayer().getWorld().getName()) && e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
			// ȡ���¼�
			e.setCancelled(true);
			
			// ��ȡ����
			FlowerPot flowerPot = (FlowerPot) e.getBlock().getState();
			// ���л�����
			String location = LocationUtils.fromString(e.getBlock().getLocation()).replace(".", "_");
			
			// �����ƻ�ʱ��
			data.set(location + ".break", System.currentTimeMillis());
			// �������Ƿ�������
			if (flowerPot.getContents() != null && !flowerPot.getContents().getItemType().equals(Material.AIR)) {
				// ��ȡ��Ʒ
				ItemStack contents = flowerPot.getContents().toItemStack();
				// ���û�������
				data.set(location + ".content", contents.getType().toString() + ":" + contents.getDurability());
			}

			// ɾ����������
			flowerPot.setContents(new MaterialData(Material.AIR));
			flowerPot.update(false, false);
			
			// ɾ������
			e.getBlock().setType(Material.AIR);
			
			// ������Ʒ
			for (String dropStr : getConfig().getStringList("Drops")) {
				// ����
				if (NumberUtils.getRand().nextDouble() < Double.valueOf(dropStr.split(" ")[0])) {
					// ������Ʒ
					ItemStack drop = ItemUtils.getCacheItem(dropStr.split(" ")[1]);
					// �����Ʒ����
					if (drop != null) {
						e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0, 0.5), drop);
					}
				}
			}
		}
	}
	

	/**
	 * ���»���
	 * 
	 * @param force ǿ��?
	 */
	@SuppressWarnings("deprecation")
	public void reset(boolean force) {
		for (String locationStr : data.getConfigurationSection("").getKeys(false)) {
			// �Ƿ���Ը���
			if (force || System.currentTimeMillis() - data.getLong(locationStr + ".break") >= getConfig().getInt("UpdateDelay") * 1000) {
				// ��ȡ����
				Block block = LocationUtils.toString(locationStr.replace("_", ".")).getBlock();
				// ���÷���
				block.setType(Material.FLOWER_POT);
				// ���������
				if (data.contains(locationStr + ".content")) {
					FlowerPot flowerPot = (FlowerPot) block.getState();
					
					// ��ȡ����
					Material material = ItemUtils.asMaterial(data.getString(locationStr + ".content").split(":")[0]);
					byte _byte = Byte.valueOf(data.getString(locationStr + ".content").split(":")[1]);
					
					// ��������
					flowerPot.setContents(new MaterialData(material, _byte));
					flowerPot.update(false, false);
				}
				// ɾ������
				data.set(locationStr, null);
				// ��������
				EffLib.CLOUD.display(0.5f, 0.5f, 0.5f, 0, 10, block.getLocation().add(0.5, 0, 0.5), 20);
			}
		}
	}
}
