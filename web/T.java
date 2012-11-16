/**
 * 
 */
package web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.GeometryBuilder;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import vo.MyPoint;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vividsolutions.jts.geom.Point;

/**
 * @author lin
 *
 */
public class T extends HttpServlet{
	
	public T(){}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String pointStr=req.getParameter("points"); 
		Gson gson=new Gson();
		java.lang.reflect.Type type= new TypeToken<List<MyPoint>>(){}.getType();
		List<MyPoint> points= gson.fromJson(pointStr,type);
		try {
			addPoint(points);
			resp.getWriter().write("添加成功");
		} catch (Exception e) {
			resp.getWriter().write(e.getMessage());
			e.printStackTrace();
		}
	}

	public void addPoint(List<MyPoint> points) throws Exception{
		File file = new File(Query.getPath()+"/shp/station.shp");
		Map map = new HashMap();
		map.put( "url", file.toURL() );
		DataStore dataStore = DataStoreFinder.getDataStore(map);

		SimpleFeatureCollection featureColl=FeatureCollections.newCollection();
		SimpleFeatureSource featureSource=dataStore.getFeatureSource("station");
		
		SimpleFeatureType featureType=featureSource.getSchema();
		SimpleFeatureBuilder featureBuilder=new SimpleFeatureBuilder(featureType);
		GeometryBuilder gb=new GeometryBuilder();
		
		for(MyPoint mpoint:points){
			Point point=gb.point(mpoint.getLon(),mpoint.getLat());
			featureBuilder.add(point);
			featureBuilder.add(1);
			featureBuilder.add(mpoint.getType());
			SimpleFeature feature=featureBuilder.buildFeature(null);
			featureColl.add(feature);
		}
		
		
		
		Transaction insertT=new DefaultTransaction("insert");
		SimpleFeatureStore featureStore=(SimpleFeatureStore)featureSource;
		featureStore.setTransaction(insertT);
		
		featureStore.addFeatures(featureColl);
		insertT.commit();
		insertT.close();
	}

	@Test
	public void test(){
		String str="{[{lon:113.187326875,lat:24.316429921875,type:'u'},{lon:114.417795625,lat:23.327660390625,type:'a'},{lon:112.56110617187,lat:23.020043203125,type:'b'}]}";
		java.lang.reflect.Type type= new TypeToken<List<MyPoint>>(){}.getType();
		Gson gson=new Gson();
		List<MyPoint> points= gson.fromJson(str,type);
	}
	
	public void te() throws Exception{
		
		MyPoint p=new MyPoint();
		p.setLat(1);
		p.setLon(2);
		p.setType("abc");
		
		Gson gson=new Gson();
		System.out.println(gson.toJson(p));
		
		File file = new File("F:/temdata/newjob/station.shp");
		Map map = new HashMap();
		map.put( "url", file.toURL() );
		DataStore dataStore = DataStoreFinder.getDataStore(map);

		SimpleFeatureCollection featureColl=FeatureCollections.newCollection();
		SimpleFeatureSource featureSource=dataStore.getFeatureSource("station");
		
		SimpleFeatureType featureType=featureSource.getSchema();
		GeometryBuilder gb=new GeometryBuilder();
		Point point=gb.point(109.499,23.44);
		SimpleFeatureBuilder featureBuilder=new SimpleFeatureBuilder(featureType);
		featureBuilder.add(point);
		featureBuilder.add(1);
		featureBuilder.add("b");
		SimpleFeature feature=featureBuilder.buildFeature(null);
		featureColl.add(feature);
		
		Transaction insertT=new DefaultTransaction("insert");
		SimpleFeatureStore featureStore=(SimpleFeatureStore)featureSource;
		featureStore.setTransaction(insertT);
		
		featureStore.addFeatures(featureColl);
		insertT.commit();
		insertT.close();
		
	}
	
}
