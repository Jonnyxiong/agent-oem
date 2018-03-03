/**
 * lpjLiu 2017-05-25
 */
package com.ucpaas.sms.util;

import com.ucpaas.sms.model.Menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommonUtils {
	public static final ConcurrentHashMap<Long, String> titleMap = new ConcurrentHashMap<Long, String>();

	public static final Menu toTree(List<Menu> menus) {
		Menu result = new Menu();
		result.setMenu_id(0L);
		Map<Long, Menu> map = new HashMap<Long, Menu>();
		map.put(0l, result);
		Long mid = null;
		if (null != menus && !menus.isEmpty()) {
			Menu tmp = null;
			List<Menu> noadd = new ArrayList<Menu>();
			for (Menu menu : menus) {
				map.put(menu.getMenu_id(), menu);
				tmp = map.get(menu.getParent_id());
				if (null != tmp) {
					tmp.addSubMenu(menu);
				} else {
					noadd.add(menu);
				}
			}
			for (Menu menu : noadd) {
				tmp = map.get(menu.getParent_id());
				if (null != tmp) {
					tmp.addSubMenu(menu);
				}
			}
			StringBuffer sb = new StringBuffer();
			boolean isLast = false;
			for (Menu menu : menus) {
				mid = menu.getMenu_id();
				if (!titleMap.containsKey(mid)) {
					sb.setLength(0);
					tmp = menu;
					isLast = true;
					do {
						sb.insert(0, "<a " + (isLast ? "class=\"current\"" : "") + " >" + tmp.getMenu_name() + "</a>");
						isLast = false;
					} while (null != (tmp = map.get(tmp.getParent_id())) && !result.equals(tmp));
					titleMap.put(menu.getMenu_id(), sb.toString());
				}
			}

		}
		return result;
	}
}
