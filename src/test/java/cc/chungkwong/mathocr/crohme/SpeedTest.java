/*
 * Copyright (C) 2019 Chan Chung Kwong
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package cc.chungkwong.mathocr.crohme;
import cc.chungkwong.mathocr.common.format.*;
import cc.chungkwong.mathocr.offline.extractor.*;
import java.awt.image.*;
import java.io.*;
import java.lang.management.*;
import java.util.logging.*;
import java.util.stream.*;
/**
 *
 * @author Chan Chung Kwong
 */
public class SpeedTest{
	public static void test(Stream<BufferedImage> imagesStream){
		ThreadMXBean threadMXBean=ManagementFactory.getThreadMXBean();
		AsciiFormat asciiFormat=new AsciiFormat();
		long wallTime=System.currentTimeMillis();
		System.out.println(imagesStream.parallel().mapToLong((image)->{
			long startTime=threadMXBean.getCurrentThreadCpuTime();
			try{
				asciiFormat.write(Extractor.DEFAULT.extract(image),null);
			}catch(IOException ex){
				Logger.getLogger(SpeedTest.class.getName()).log(Level.SEVERE,null,ex);
			}
			return threadMXBean.getCurrentThreadCpuTime()-startTime;
		}).summaryStatistics());
		System.out.println("Wall time(ms): "+(System.currentTimeMillis()-wallTime));
		System.out.println(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
	}
	public static void main(String[] args){
		test(CrohmeOffline.getValidationStream2016Image());
		test(CrohmeOffline.getTestStream2016Image());
		test(CrohmeOffline.getTestStream2019Image());
	}
}
