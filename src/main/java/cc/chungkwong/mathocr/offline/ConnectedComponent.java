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
package cc.chungkwong.mathocr.offline;
import cc.chungkwong.mathocr.common.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
/**
 * A data structure represent a connented component
 */
public final class ConnectedComponent implements Externalizable,Comparable<ConnectedComponent>{
	private static final long serialVersionUID=1L;
	public static final Comparator<ConnectedComponent> FROM_LEFT=Comparator.<ConnectedComponent>comparingInt((c)->c.cordLeft).
			thenComparingInt((c)->c.cordTop).thenComparingInt((c)->c.cordRight);
	public int cordTop=Integer.MAX_VALUE, cordLeft=Integer.MAX_VALUE, cordBottom=0, cordRight=0;
	private final List<RunLength> runlengths=new LinkedList<>();
	/**
	 * Construct a empty ConnectedComponent
	 */
	public ConnectedComponent(){
	}
	/**
	 * Construct a empty ConnectedComponent
	 *
	 * @param image Binarized image
	 */
	public ConnectedComponent(BufferedImage image){
		int width=image.getWidth(), height=image.getHeight();
		int[] rgb=image.getRGB(0,0,width,height,null,0,width);
		for(int i=0, ind=0;i<height;i++){
			int blackStart=-1;
			for(int j=0;j<width;j++,ind++){
				if((rgb[ind]&0xFFFFFF)==0){
					if(blackStart==-1){
						blackStart=j;
					}
				}else if(blackStart!=-1){
					addRunLengthToLast(new RunLength(i,blackStart,j-blackStart-1));
					blackStart=-1;
				}
			}
			if(blackStart!=-1){
				addRunLengthToLast(new RunLength(i,blackStart,width-blackStart-1));
			}
		}
	}
	/**
	 * Construct a empty ConnectedComponent with given bounds
	 *
	 * @param box
	 */
	public ConnectedComponent(BoundBox box){
		this(box.getLeft(),box.getRight(),box.getTop(),box.getBottom());
		//addRunLength(new RunLength(cordTop,cordLeft,cordRight-cordLeft));
		//addRunLength(new RunLength(cordBottom,cordLeft,cordRight-cordLeft));
	}
	/**
	 * Construct a empty ConnectedComponent with given bounds
	 *
	 * @param cordLeft left bound
	 * @param cordRight right bound
	 * @param cordTop upper bound
	 * @param cordBottom lower bound
	 */
	public ConnectedComponent(int cordLeft,int cordRight,int cordTop,int cordBottom){
		this.cordLeft=cordLeft;
		this.cordRight=cordRight;
		this.cordTop=cordTop;
		this.cordBottom=cordBottom;
		//addRunLength(new RunLength(cordTop,cordLeft,cordRight-cordLeft));
		//addRunLength(new RunLength(cordBottom,cordLeft,cordRight-cordLeft));
	}
	/**
	 * Construct a ConnectedComponent with a single RunLength
	 *
	 * @param runlength the RunLength
	 */
	public ConnectedComponent(RunLength runlength){
		addRunLength(runlength);
	}
	/**
	 * Add a RunLength
	 *
	 * @param runlength the RunLength to be added
	 */
	public void addRunLength(RunLength runlength){
		ListIterator<RunLength> iter=runlengths.listIterator();
		while(iter.hasNext()){
			if(iter.next().compareTo(runlength)>0){
				iter.previous();
				break;
			}
		}
		iter.add(runlength);
		if(runlength.getY()<cordTop){
			cordTop=runlength.getY();
		}
		if(runlength.getX()<cordLeft){
			cordLeft=runlength.getX();
		}
		if(runlength.getY()>cordBottom){
			cordBottom=runlength.getY();
		}
		if(runlength.getX()+runlength.getCount()>cordRight){
			cordRight=runlength.getX()+runlength.getCount();
		}
	}
	/**
	 * Add a RunLength provided that it is greater than all the RunLength
	 * already in the ConnectedComponent this method is added to enhance
	 * performance of connected component analysis
	 *
	 * @param runlength the RunLength to be added
	 */
	public void addRunLengthToLast(RunLength runlength){
		runlengths.add(runlength);
		if(runlength.getY()<cordTop){
			cordTop=runlength.getY();
		}
		if(runlength.getX()<cordLeft){
			cordLeft=runlength.getX();
		}
		if(runlength.getY()>cordBottom){
			cordBottom=runlength.getY();
		}
		if(runlength.getX()+runlength.getCount()>cordRight){
			cordRight=runlength.getX()+runlength.getCount();
		}
	}
	/**
	 * Get the runlengths in the component
	 *
	 * @return the runlengths
	 */
	public List<RunLength> getRunLengths(){
		return runlengths;
	}
	/**
	 * Merge this component with c
	 *
	 * @param c the component to merge
	 */
	public void combineWith(ConnectedComponent c){
		ListIterator<RunLength> iter=runlengths.listIterator(), iterc=c.runlengths.listIterator();
		RunLength currc=iterc.hasNext()?iterc.next():null;
		while(currc!=null){
			if(iter.hasNext()){
				if(iter.next().compareTo(currc)>0){
					iter.previous();
					iter.add(currc);
					currc=iterc.hasNext()?iterc.next():null;
				}
			}else{
				iter.add(currc);
				while(iterc.hasNext()){
					iter.add(iterc.next());
				}
				break;
			}
		}
		if(c.cordTop<cordTop){
			cordTop=c.cordTop;
		}
		if(c.cordLeft<cordLeft){
			cordLeft=c.cordLeft;
		}
		if(c.cordBottom>cordBottom){
			cordBottom=c.cordBottom;
		}
		if(c.cordRight>cordRight){
			cordRight=c.cordRight;
		}
	}
	/**
	 * Form a ConnectedComponent by combining a list of ConnectedComponent
	 *
	 * @param c the ConnectedComponent to be combined
	 * @return combination result
	 */
	public static ConnectedComponent combine(List<ConnectedComponent> c){
		//can be improved using binary merge
		ConnectedComponent ele=new ConnectedComponent();
		for(ConnectedComponent g:c){
			ListIterator<RunLength> iter=ele.runlengths.listIterator(), iterc=g.runlengths.listIterator();
			RunLength currc=iterc.hasNext()?iterc.next():null;
			while(currc!=null){
				if(iter.hasNext()){
					if(iter.next().compareTo(currc)>0){
						iter.previous();
						iter.add(currc);
						currc=iterc.hasNext()?iterc.next():null;
					}
				}else{
					iter.add(currc);
					while(iterc.hasNext()){
						iter.add(iterc.next());
					}
					break;
				}
			}
			if(g.cordTop<ele.cordTop){
				ele.cordTop=g.cordTop;
			}
			if(g.cordLeft<ele.cordLeft){
				ele.cordLeft=g.cordLeft;
			}
			if(g.cordBottom>ele.cordBottom){
				ele.cordBottom=g.cordBottom;
			}
			if(g.cordRight>ele.cordRight){
				ele.cordRight=g.cordRight;
			}
		}
		return ele;
	}
	/**
	 * Split this component horizontally
	 *
	 * @param x the coordinate of the line to be used to split
	 * @return the right one
	 */
	public ConnectedComponent splitHorizontally(int x){
		ConnectedComponent ele=new ConnectedComponent();
		Iterator<RunLength> iter=runlengths.iterator();
		cordTop=Integer.MAX_VALUE;
		cordBottom=0;
		while(iter.hasNext()){
			RunLength length=iter.next();
			if(length.getX()>x){
				ele.addRunLengthToLast(length);
				iter.remove();
			}else{
				if(length.getX()+length.getCount()>x){
					ele.addRunLengthToLast(new RunLength(length.getY(),x+1,length.getX()+length.getCount()-x-1));
					length.reset(length.getY(),length.getX(),x-length.getX());
				}
				if(length.getY()<cordTop){
					cordTop=length.getY();
				}
				if(length.getY()>cordBottom){
					cordBottom=length.getY();
				}
			}
		}
		cordRight=x;
		return ele;
	}
	/**
	 * Split this component vertically
	 *
	 * @param y the coordinate of the line to be used to split
	 * @return the lower one
	 */
	public ConnectedComponent splitVertically(int y){
		ConnectedComponent ele=new ConnectedComponent();
		Iterator<RunLength> iter=runlengths.iterator();
		cordLeft=Integer.MAX_VALUE;
		cordRight=0;
		while(iter.hasNext()){
			RunLength length=iter.next();
			if(length.getY()>y){
				ele.addRunLengthToLast(length);
				iter.remove();
			}else{
				if(length.getX()<cordLeft){
					cordLeft=length.getX();
				}
				if(length.getX()+length.getCount()>cordRight){
					cordRight=length.getX()+length.getCount();
				}
			}
		}
		cordBottom=y;
		return ele;
	}
	/**
	 * Get the position of the top of the component in the image
	 *
	 * @return the coordinate
	 */
	public int getTop(){
		return cordTop;
	}
	/**
	 * Get the position of the bottom of the component in the image
	 *
	 * @return the coordinate
	 */
	public int getBottom(){
		return cordBottom;
	}
	/**
	 * Get the position of the left of the component in the image
	 *
	 * @return the coordinate
	 */
	public int getLeft(){
		return cordLeft;
	}
	/**
	 * Get the position of the right of the component in the image
	 *
	 * @return the coordinate
	 */
	public int getRight(){
		return cordRight;
	}
	/**
	 * Get the width
	 *
	 * @return width
	 */
	public int getWidth(){
		return cordRight>=cordLeft?cordRight-cordLeft+1:0;
	}
	/**
	 * Get the height
	 *
	 * @return height
	 */
	public int getHeight(){
		return cordBottom>=cordTop?cordBottom-cordTop+1:0;
	}
	/**
	 * Compute horizontal crossing numbers
	 *
	 * @return horizontal crossing numbers
	 */
	public byte[] getHorizontalCrossing(){
		byte[] cross=new byte[getHeight()];
		for(RunLength rl:runlengths){
			++cross[rl.getY()-cordTop];
		}
		return cross;
	}
	/**
	 * Compute vertical crossing numbers
	 *
	 * @return vertical crossing numbers
	 */
	public byte[] getVerticalCrossing(){
		byte[] cross=new byte[getWidth()], prev=new byte[getWidth()];
		Iterator<RunLength> iter=runlengths.iterator();
		RunLength curr=iter.hasNext()?iter.next():null;
		for(int i=cordTop;i<=cordBottom;i++){
			while(curr!=null&&curr.getY()==i){
				for(int j=0, k=curr.getX()-cordLeft;j<=curr.getCount();j++,k++){
					if(prev[k]==0){
						++cross[k];
					}
					prev[k]=-1;
				}
				curr=iter.hasNext()?iter.next():null;
			}
			for(int k=0;k<prev.length;k++){
				prev[k]=(byte)(prev[k]==-1?1:0);
			}
		}
		return cross;
	}
	/**
	 * Compute power
	 *
	 * @param base the base
	 * @param exp the exponent
	 * @return the power
	 */
	private double pow(double base,int exp){
		double re=1;
		while(--exp>=0){
			re*=base;
		}
		return re;
	}
	/**
	 * Get the moment
	 *
	 * @param p the order of x
	 * @param q the order of y
	 * @return the moment
	 */
	public double getMoment(int p,int q){
		double moment=0;
		int count=0;
		for(RunLength rl:runlengths){
			int y=rl.getY()-cordTop, x=rl.getX()-cordLeft-1;
			for(int j=0;j<=rl.getCount();j++){
				moment+=pow(++x,p)*pow(y,q);
				++count;
			}
		}
		return moment/count;
	}
	/**
	 * Get the standardized moment
	 *
	 * @param p the order of x
	 * @param q the order of y
	 * @return the moment
	 */
	public double getCentralMoment(int p,int q){
		double moment=0;
		int count=0;
		double xmean=getMoment(1,0)+cordLeft, ymean=getMoment(0,1)+cordTop;
		for(RunLength rl:runlengths){
			double y=rl.getY()-ymean, x=rl.getX()-xmean-1;
			for(int j=0;j<=rl.getCount();j++){
				moment+=pow(++x,p)*pow(y,q);
				++count;
			}
		}
		return moment/Math.pow(count,(p+q)*0.5+1);
		//return moment/pow(getWidth(),p)/pow(getHeight(),q)/count;
	}
	/**
	 * Get the horizontal center
	 *
	 * @return the horizontal center
	 */
	public double getCenterX(){
		return getMoment(1,0)/getWidth();
	}
	/**
	 * Get the vertical center
	 *
	 * @return the vertical center
	 */
	public double getCenterY(){
		return getMoment(0,1)/getHeight();
	}
	/**
	 * Get the direction
	 *
	 * @return direction
	 */
	public double getDirection(){
		return 0.5f*Math.atan2(2*getCentralMoment(1,1),getCentralMoment(2,0)-getCentralMoment(0,2));
	}
	/**
	 * Get the density
	 *
	 * @return density
	 */
	public double getDensity(){
		return ((double)getWeight())/getBox().getArea();
	}
	/**
	 * Get the Weight
	 *
	 * @return weight
	 */
	public int getWeight(){
		int total=0;
		for(RunLength rl:runlengths){
			total+=rl.getCount()+1;
		}
		return total;
	}
	/**
	 * Get the number of holes
	 *
	 * @return number of holes
	 */
	public int getNumberOfHoles(){
		int width=getWidth(), height=getHeight();
		int holes=-1;
		int curr_id=0;
		int[] label=new int[width+2];
		ArrayList<Integer> parent=new ArrayList<>();
		parent.add(-1);
		Iterator<RunLength> iter=runlengths.iterator();
		RunLength curr=iter.hasNext()?iter.next():null;
		for(int i=0;i<height;i++){
			int[] tmp=new int[width+2];
			while(curr!=null&&curr.getY()-cordTop==i){
				for(int j=curr.getX()-cordLeft+1, k=0;k<=curr.getCount();j++,k++){
					tmp[j]=-1;
				}
				curr=iter.hasNext()?iter.next():null;
			}
			for(int j=1;j<width+2;j++){
				if(tmp[j]==0){
					TreeSet<Integer> neighbour=new TreeSet<>();
					if(label[j-1]>=0){
						neighbour.add(label[j-1]);
					}
					if(label[j]>=0){
						neighbour.add(label[j]);
					}
					if(j<=width&&label[j+1]>=0){
						neighbour.add(label[j+1]);
					}
					if(tmp[j-1]>=0){
						neighbour.add(tmp[j-1]);
					}
					if(neighbour.isEmpty()){
						parent.add(-1);
						tmp[j]=++curr_id;
					}else{
						Integer first=neighbour.first();
						tmp[j]=first;
						Integer next=first;
						while((next=neighbour.higher(next))!=null){
							parent.set(next,first);
						}
					}
				}
			}
			/*for(int j=1;j<width+2;j++)
				if(tmp[j]==0)
					if(label[j]>=0&&tmp[j-1]>=0){
						if(label[j]==tmp[j-1])
							tmp[j]=tmp[j-1];
						else if(label[j]<tmp[j-1]){
							parent.set(tmp[j-1],label[j]);
							tmp[j]=label[j];
						}else{
							parent.set(label[j],tmp[j-1]);
							tmp[j]=tmp[j-1];
						}
					}else if(label[j]>=0&&tmp[j-1]<0)
						tmp[j]=label[j];
					else if(label[j]<0&&tmp[j-1]>=0)
						tmp[j]=tmp[j-1];
					else if(label[j]<0&&tmp[j-1]<0){
						parent.add(-1);
						tmp[j]=++curr_id;
					}*/
			label=tmp;
		}
		for(int j=1;j<width+1;j++){
			if(label[j]>0){
				parent.set(label[j],0);
			}
		}
		for(Integer par:parent){
			if(par==-1){
				++holes;
			}
		}
		return holes;
	}
	/**
	 * Get two dimension pixels array representation
	 *
	 * @return pixels array
	 */
	public byte[][] toPixelArray(){
		byte[][] pixels=new byte[getHeight()][getWidth()];
		for(RunLength rl:runlengths){
			int i=rl.getY()-cordTop, j=rl.getX()-cordLeft-1;
			for(int k=0;k<=rl.getCount();k++){
				pixels[i][++j]=1;
			}
		}
		return pixels;
	}
	/**
	 * Get one dimension pixels array representation
	 *
	 * @return pixels array
	 */
	public byte[] toPixelArray2(){
		byte[] pixels=new byte[getHeight()*getWidth()];
		int width=getWidth();
		for(RunLength rl:runlengths){
			int i=rl.getY()-cordTop, j=rl.getX()-cordLeft-1;
			for(int k=0;k<=rl.getCount();k++){
				pixels[i*width+(++j)]=1;
			}
		}
		return pixels;
	}
	/**
	 * Get String that show the shape
	 *
	 * @return the string that show the shape
	 */
	@Override
	public String toString(){
		byte[][] pixels=toPixelArray();
		if(pixels.length==0){
			return "";
		}
		int height=pixels.length, width=pixels[0].length;
		StringBuilder str=new StringBuilder((width+1)*height);
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
				str.append(pixels[i][j]);
			}
			str.append('\n');
		}
		return str.toString();
	}
	/**
	 * Write this object to stream
	 *
	 * @param out output stream
	 * @throws java.io.IOException
	 */
	@Override
	public void writeExternal(ObjectOutput out) throws IOException{
		out.writeByte(cordRight-cordLeft);
		out.writeByte(cordBottom-cordTop);
		for(RunLength rl:runlengths){
			out.writeByte(rl.getY()-cordTop);
			out.writeByte(rl.getX()-cordLeft);
			out.writeByte(rl.getCount());
		}
		out.writeByte(255);
	}
	/**
	 * Load ConnectedComponent from stream
	 *
	 * @param in input stream
	 * @throws java.io.IOException
	 */
	@Override
	public void readExternal(ObjectInput in) throws IOException{
		cordLeft=0;
		cordTop=0;
		cordRight=in.readUnsignedByte();
		cordBottom=in.readUnsignedByte();
		while(true){
			int first=in.readUnsignedByte();
			if(first==255){
				break;
			}
			runlengths.add(new RunLength(first,in.readUnsignedByte(),in.readUnsignedByte()));
		}
	}
	/**
	 * Compare first runlength of ConnectedComponent
	 *
	 * @param ele to be compared to
	 * @return a integer
	 */
	@Override
	public int compareTo(ConnectedComponent ele){
		int compare=Integer.compare(cordTop,ele.cordTop);
		if(compare!=0){
			return compare;
		}
		compare=Integer.compare(cordLeft,ele.cordLeft);
		if(compare!=0){
			return compare;
		}else{
			return Integer.compare(cordBottom,ele.cordBottom);
		}
	}
	public BoundBox getBox(){
		return new BoundBox(cordLeft,cordRight,cordTop,cordBottom);
	}
	public void fix(){
		Collections.sort(runlengths);
		ListIterator<RunLength> iterator=runlengths.listIterator();
		if(iterator.hasNext()){
			RunLength prev=iterator.next();
			while(iterator.hasNext()){
				RunLength curr=iterator.next();
				if(curr.getY()==prev.getY()&&curr.getX()<=prev.getX()+prev.getCount()){
					prev.reset(prev.getY(),prev.getX(),Math.max(prev.getX()+prev.getCount(),curr.getX()+curr.getCount())-prev.getX());
					iterator.remove();
				}else{
					prev=curr;
				}
			}
		}
	}
}
