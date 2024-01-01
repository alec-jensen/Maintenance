/*
 * This file is part of Maintenance - https://github.com/kennytv/Maintenance
 * Copyright (C) 2018-2024 kennytv (https://github.com/kennytv)
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
package eu.kennytv.maintenance.core.proxy.command.subcommand;

import eu.kennytv.maintenance.api.proxy.Server;
import eu.kennytv.maintenance.core.proxy.MaintenanceProxyPlugin;
import eu.kennytv.maintenance.core.proxy.command.ProxyCommandInfo;
import eu.kennytv.maintenance.core.runnable.MaintenanceRunnableBase;
import eu.kennytv.maintenance.core.util.SenderInfo;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class SingleEndtimerCommand extends ProxyCommandInfo {

    public SingleEndtimerCommand(final MaintenanceProxyPlugin plugin) {
        super(plugin, null);
    }

    @Override
    public boolean hasPermission(final SenderInfo sender) {
        return sender.hasMaintenancePermission("timer") || sender.hasPermission("maintenance.singleserver.timer");
    }

    @Override
    public void execute(final SenderInfo sender, final String[] args) {
        if (args.length == 2) {
            if (checkPermission(sender, "timer")) return;
            if (plugin.getCommandManager().checkTimerArgs(sender, args[1])) {
                sender.send(getHelpMessage());
                return;
            }
            if (!plugin.isMaintenance()) {
                sender.send(getMessage("alreadyDisabled"));
                return;
            }

            plugin.startMaintenanceRunnable(Integer.parseInt(args[1]), TimeUnit.MINUTES, false);
            sender.send(getMessage("endtimerStarted", "%TIME%", plugin.getRunnable().getTime()));
        } else if (args.length == 3) {
            if (checkPermission(sender, "singleserver.timer")) return;
            if (plugin.getCommandManager().checkTimerArgs(sender, args[2], false)) {
                sender.send(getHelpMessage());
                return;
            }

            final Server server = plugin.getCommandManager().checkSingleTimerServerArg(sender, args[1]);
            if (server == null) return;
            if (!plugin.isMaintenance(server)) {
                sender.send(getMessage("singleServerAlreadyDisabled", "%SERVER%", server.getName()));
                return;
            }

            final MaintenanceRunnableBase runnable = plugin.startSingleMaintenanceRunnable(server, Integer.parseInt(args[2]), TimeUnit.MINUTES, false);
            sender.send(getMessage(
                    "singleEndtimerStarted",
                    "%TIME%", runnable.getTime(),
                    "%SERVER%", server.getName()
            ));
        } else {
            sender.send(getHelpMessage());
        }
    }

    @Override
    public List<String> getTabCompletion(final SenderInfo sender, final String[] args) {
        return args.length == 2 && sender.hasMaintenancePermission("singleserver.timer") ? plugin.getCommandManager().getMaintenanceServersCompletion(args[1].toLowerCase()) : Collections.emptyList();
    }
}
