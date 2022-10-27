package com.github.smallru8.nikobot.rolemanager;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.github.smallru8.NikoBot.Core;
import com.github.smallru8.NikoBot.StdOutput;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class RoleManager extends ListenerAdapter{

	private File conf_dir;
	private JSONObject mapping;
	private String announcement = "";
	
	public RoleManager(String managerName) {
		conf_dir = new File("conf.d/role/"+managerName);
		if(!conf_dir.exists())
			conf_dir.mkdir();
		init();
		if(!checkSetting()) {
			StdOutput.errorPrintln("[RoleManager]["+managerName+"] Config not set.");
			System.exit(0);
		}
		checkAnnouncement();
	}
	
	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {//user訂閱role
		if(event.getMessageId().equalsIgnoreCase(getMessageId())) {
			String[] roles = getRolebyReaction(event.getEmoji().getAsReactionCode());
			Member member = event.getMember();
			for(String role_id:roles) {
				Role role = Core.botAPI.getGuildById(getGuild()).getRoleById(role_id);
				if(role!=null)
					Core.botAPI.getGuildById(getGuild()).addRoleToMember(member, role).queue();
			}
		}
	}
	
	@Override
	public void onMessageReactionRemove(MessageReactionRemoveEvent event) {//user取消訂閱role
		event.retrieveMember().queue(member -> {
			if(event.getMessageId().equalsIgnoreCase(getMessageId())) {
				String[] roles = getRolebyReaction(event.getEmoji().getAsReactionCode());
				for(String role_id:roles) {
					Role role = Core.botAPI.getGuildById(getGuild()).getRoleById(role_id);
					if(role!=null)
						Core.botAPI.getGuildById(getGuild()).removeRoleFromMember(member, role).queue();
				}
			}
		});
	}
	
	@Override
	public void onGuildMemberRemove(GuildMemberRemoveEvent event) {//user離開伺服器
		User user = event.getUser();
		Core.botAPI.getGuildById(getGuild()).getTextChannelById(getChannel()).retrieveMessageById(getMessageId()).queue(m -> {
			m.getReactions().forEach(action -> {
				action.removeReaction(user).queue();
			});
		});
	}
	
	private String getGuild() {
		return mapping.getString("guildId");
	}
	
	private String getChannel() {
		return mapping.getString("channelId");
	}
	
	private String getMessageId() {
		return mapping.getString("messageId");
	}
	
	private void setMessageId(String id) {
		mapping.put("messageId", id);
		File f = new File(conf_dir,"roleMapping.json");
		try {
			FileWriter fw = new FileWriter(f);
			fw.write(mapping.toString());
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String[] getRolebyReaction(String emoji) {
		try {
			List<Object> ls = mapping.getJSONObject("reactionRoleMapping").getJSONArray(emoji).toList();
			String[] role_ls = new String[ls.size()];
			for(int i=0;i<ls.size();i++) {
				role_ls[i] = (String)ls.get(i);
			}
			return role_ls;
		}catch(JSONException e) {
			return new String[0];
		}
	}
	//=======================================================================
	private void init() {
		String role_json = "{\r\n"
				+ "	\"guildId\":\"\",\r\n"
				+ "	\"channelId\":\"\",\r\n"
				+ "	\"messageId\":\"\",\r\n"
				+ "	\"reactionRoleMapping\":{\r\n"
				+ "		\"✅\":[\"1031872781366853683\"]\r\n"
				+ "	}\r\n"
				+ "}";
		
		try {
			File f = new File(conf_dir,"roleMapping.json");
			if(!f.exists()) {
				FileWriter fw = new FileWriter(f);
				fw.write(role_json);
				fw.flush();
				fw.close();
			}
			
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			String line = "",sum = "";
			while((line=br.readLine())!=null) {
				sum+=line;
			}
			br.close();
			fr.close();
			mapping = new JSONObject(sum);
			
			f = new File(conf_dir,"announcement.txt");
			if(!f.exists())
				f.createNewFile();
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			line = "";
			while((line=br.readLine())!=null) {
				announcement+=line+"\n";
			}
			br.close();
			fr.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean checkSetting() {
		try {
			if(mapping.getString("guildId").equalsIgnoreCase("")) return false;
			if(mapping.getString("channelId").equalsIgnoreCase("")) return false;
		}catch(Exception e) {
			return false;
		}
		return true;
	}
	
	private void checkAnnouncement() {
		if(mapping.getString("messageId").equalsIgnoreCase("")) {
			EmbedBuilder embed = new EmbedBuilder();
			embed.setColor(Color.PINK);
			embed.setTitle(":bookmark: Announcement");
			embed.setDescription(announcement);
			Core.botAPI.getGuildById(getGuild()).getTextChannelById(getChannel()).sendMessageEmbeds(embed.build()).queue(m->{
				setMessageId(m.getId());
				Iterator<String> key_it = mapping.getJSONObject("reactionRoleMapping").keys();
				while(key_it.hasNext()) {
					m.addReaction(Emoji.fromUnicode(key_it.next())).queue();
				}
			});
		}
	}
	
}