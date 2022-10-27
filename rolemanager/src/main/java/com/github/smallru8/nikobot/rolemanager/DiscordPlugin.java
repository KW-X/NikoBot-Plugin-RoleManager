package com.github.smallru8.nikobot.rolemanager;

import com.github.smallru8.NikoBot.Core;
import com.github.smallru8.NikoBot.plugins.PluginsInterface;

public class DiscordPlugin implements PluginsInterface{

	@Override
	public void onDisable() {
		
	}

	@Override
	public void onEnable() {
		Core.botAPI.addEventListener(new RoleManager("main"));
	}

	@Override
	public String pluginsName() {
		return "RoleManager";
	}

}
