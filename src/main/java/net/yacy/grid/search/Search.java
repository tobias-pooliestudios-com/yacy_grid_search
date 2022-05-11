/**
 *  Search Client
 *  Copyright 05.11.2018 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.grid.search;

import java.util.Properties;

import net.yacy.grid.YaCyServices;
import net.yacy.grid.mcp.Configuration;
import net.yacy.grid.mcp.MCP;
import net.yacy.grid.mcp.Service;
import net.yacy.grid.tools.CronBox;
import net.yacy.grid.tools.CronBox.Telemetry;
import net.yacy.grid.tools.GitTool;
import net.yacy.grid.tools.Logger;

/**
 * The Search Client main class
 *
 * You can search at the following API:
 * http://localhost:8800/yacy/grid/mcp/index/yacysearch.json?query=*
 * http://localhost:8800/yacy/grid/mcp/index/gsasearch.xml?q=*
 *
 * performance debugging:
 * http://localhost:8800/yacy/grid/mcp/info/threaddump.txt
 * http://localhost:8800/yacy/grid/mcp/info/threaddump.txt?count=100 *
 */
public class Search {

    private final static YaCyServices SEARCH_SERVICE = YaCyServices.aggregation; // check with http://localhost:8800/yacy/grid/mcp/status.json
    private final static String DATA_PATH = "data";

    public static class Application implements CronBox.Application {

        final Configuration config;
        final Service service;
        final CronBox.Application serviceApplication;

        public Application() {
            Logger.info("Starting Search Application...");

            // initialize configuration
            this.config = new Configuration(DATA_PATH, true, SEARCH_SERVICE, MCP.MCP_SERVLETS);

            // initialize REST server with services
            this.service = new Service(this.config);

            // connect backend
            this.config.connectBackend();

            // initiate service application: listening to REST request
            this.serviceApplication = this.service.newServer(null);
        }

        @Override
        public void run() {
            // starting threads
            this.serviceApplication.run(); // SIC! the service application is running as the core element of this run() process. If we run it concurrently, this runnable will be "dead".
        }

        @Override
        public void stop() {
            Logger.info("Stopping Search Application...");
            this.serviceApplication.stop();
            this.service.stop();
            this.service.close();
            this.config.close();
        }

        @Override
        public Telemetry getTelemetry() {
            return null;
        }

    }
    public static void main(final String[] args) {
        // run in headless mode
        System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff

        // prepare configuration
        final Properties sysprops = System.getProperties(); // system properties
        System.getenv().forEach((k,v) -> {
            if (k.startsWith("YACYGRID_")) sysprops.put(k.substring(9).replace('_', '.'), v);
        }); // add also environment variables

        // first greeting
        Logger.info("Search started!");
        Logger.info(new GitTool().toString());
        Logger.info("you can now search using the query api, i.e.:");
        Logger.info("curl \"http://127.0.0.1:8800/yacy/grid/mcp/index/yacysearch.json?query=test\"");

        // run application with cron
        final long cycleDelay = Long.parseLong(System.getProperty("YACYGRID_SEARCH_CYCLEDELAY", "" + Long.MAX_VALUE)); // by default, run only in one genesis thread
        final int cycleRandom = Integer.parseInt(System.getProperty("YACYGRID_SEARCH_CYCLERANDOM", "" + 1000 * 60 /*1 minute*/));
        final CronBox cron = new CronBox(Application.class, cycleDelay, cycleRandom);
        cron.cycle();

        // this line is reached if the cron process was shut down
        Logger.info("Search terminated");
    }

}
