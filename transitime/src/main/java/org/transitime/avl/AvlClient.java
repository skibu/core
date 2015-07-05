/* 
 * This file is part of Transitime.org
 * 
 * Transitime.org is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL) as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * Transitime.org is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Transitime.org .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.transitime.avl;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.transitime.configData.AvlConfig;
import org.transitime.core.AvlProcessor;
import org.transitime.db.structs.AvlReport;
import org.transitime.utils.Time;

/**
 * Receives data from the AvlJmsClientModule and processes it.
 * Can use multiple threads to handle data.
 * 
 * @author SkiBu Smith
 */
public class AvlClient implements Runnable {

	// The AVL report being processed
	private final AvlReport avlReport;

	// List of current AVL reports by vehicle. Useful for determining last
	// report so can filter out new report if the same as the old one.
	// Keyed on vehicle ID.
	private static HashMap<String, AvlReport> avlReports =
			new HashMap<String, AvlReport>();
	
	private static final Logger logger= 
			LoggerFactory.getLogger(AvlClient.class);	

	/********************** Member Functions **************************/

	/**
	 * Constructor
	 * @param avlReport
	 */
	public AvlClient(AvlReport avlReport) {
		this.avlReport = avlReport;
	}
	
	/**
	 * Filters out problematic AVL reports (such as for having invalid data,
	 * being in the past, or too recent) and processes the ones that are good.
	 * 
	 * (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// If the data is bad throw it out
		String errorMsg = avlReport.validateData();
		if (errorMsg != null) {
			logger.error("Throwing away avlReport {} because {}", 
					avlReport, errorMsg);
			return;
		}
		
		// See if should filter out report
		synchronized (avlReports) {			
			AvlReport previousReportForVehicle = 
					avlReports.get(avlReport.getVehicleId());

			// If report the same time or older then don't need to process it
			if (previousReportForVehicle != null &&
					avlReport.getTime() <= previousReportForVehicle.getTime()) {
				logger.warn("Throwing away AVL report because it is same time "
						+ "or older than the previous AVL report for the "
						+ "vehicle. New AVL report is {}. Previous valid AVL "
						+ "report is {}", 
						avlReport, previousReportForVehicle);
				return;
			}
			
			// If previous report happened too recently then don't want to 
			// process it. This is important for when get AVL data for a vehicle
			// more frequently than is worthwhile, like every couple of seconds.
			if (previousReportForVehicle != null) {
				long timeBetweenReportsSecs =
						(avlReport.getTime() - previousReportForVehicle
								.getTime()) / Time.MS_PER_SEC;
				if (timeBetweenReportsSecs < AvlConfig.getMinTimeBetweenAvlReportsSecs()) {
					// Log this but. Since this can happen very frequently 
					// (VTA has hundreds of vehicles reporting every second!) 
					// separated the logging into two statements in case want 
					// to make the first shorter one a warn message but keep the
					// second more verbose one a debug statement.
					logger.debug("AVL report for vehicleId={} for time {} is only {} "
							+ "seconds old which is too "
							+ "recent to previous report so discarding it",
							avlReport.getVehicleId(), avlReport.getTime(), 
							timeBetweenReportsSecs);
					logger.debug("Throwing away AVL report because the new report "
							+ "is too close in time to the previous AVL report "
							+ "for the vehicle. "
							+ "transitime.avl.minTimeBetweenAvlReportsSecs={} "
							+ "secs. New AVL report is {}. Previous valid AVL "
							+ "report is {}", 
							AvlConfig.getMinTimeBetweenAvlReportsSecs(), avlReport, 
							previousReportForVehicle);
					return;				
				}
			}
						
			// Should handle the avl report so remember so can possibly filter the
			// next one
			avlReports.put(avlReport.getVehicleId(), avlReport);
		}
		
		// Process the report
		logger.info("Thread={} AvlClient processing AVL data {}", 
				Thread.currentThread().getName(), avlReport);	
		AvlProcessor.getInstance().processAvlReport(avlReport);
	}
	
}