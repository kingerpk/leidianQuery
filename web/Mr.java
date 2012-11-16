package web;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollections;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.GeometryBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import util.IOUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class Mr extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}
	
	@Test
	public void test(){
		
		GeometryBuilder gb=new GeometryBuilder();
		Point p=gb.point(114, 22);
		Geometry geo=p.buffer(0.009*2,4);
		Coordinate[] coords=geo.getCoordinates();
		for(int i=0;i<coords.length;i++){
			System.out.println(coords[i]);
		}
	}


	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String datacrsCode=req.getParameter("datacrs");
		String targetcrsCode=req.getParameter("targetcrs");
		CoordinateReferenceSystem datacrs=Query.CrsMap.get(datacrsCode);
		CoordinateReferenceSystem targetcrs=Query.CrsMap.get(targetcrsCode);
		if(datacrs==null){
			try {
				datacrs=CRS.decode(datacrsCode);
				Query.CrsMap.put(datacrsCode, datacrs);
			} catch (NoSuchAuthorityCodeException e1) {
				IOUtil.pushData(resp, e1.getMessage());
				return;
			} catch (FactoryException e1) {
				IOUtil.pushData(resp, e1.getMessage());
				return;
			}
		}
		if(targetcrs==null){
			try {
				targetcrs=CRS.decode(targetcrsCode);
				Query.CrsMap.put(targetcrsCode, targetcrs);
			} catch (NoSuchAuthorityCodeException e) {
				IOUtil.pushData(resp, e.getMessage());
				return;
			} catch (FactoryException e) {
				IOUtil.pushData(resp, e.getMessage());
				return;
			}
		}
		try {
			datacrs=CRS.decode(datacrsCode);
			targetcrs=CRS.decode(targetcrsCode);
		} catch (NoSuchAuthorityCodeException e1) {
			IOUtil.pushData(resp, e1.getMessage());
			return;
		} catch (FactoryException e1) {
			IOUtil.pushData(resp, e1.getMessage());
			return;
		}
		double DpreKM=0.009;
		double KM=Double.parseDouble(req.getParameter("buffer"));
		double x=Double.parseDouble(req.getParameter("x"));
		double y=Double.parseDouble(req.getParameter("y"));
		GeometryFactory gf=new GeometryFactory();
		Point p=gf.createPoint(new Coordinate(x, y));
		Geometry geo=p.buffer(DpreKM*KM);
		try {
			SimpleFeatureCollection queryResult=getLeidianPoint(geo.toString());
			SimpleFeatureCollection newCrsSfc= FeatureCollections.newCollection();
			if(!datacrsCode.equals(targetcrsCode)){
				MathTransform transform;
				try {
					transform = CRS.findMathTransform(datacrs, targetcrs, true);
					SimpleFeatureIterator  sfi=queryResult.features();
					
					while(sfi.hasNext()){
						SimpleFeature sf1=sfi.next();
						Geometry temgeo=JTS.transform((Geometry) sf1.getDefaultGeometry(), transform);	
						sf1.setDefaultGeometry(temgeo);
						newCrsSfc.add(sf1);
					}
				} catch (FactoryException e) {
					IOUtil.pushData(resp, e.getMessage());
					return;
				} catch (MismatchedDimensionException e) {
					IOUtil.pushData(resp, e.getMessage());
					return;
				} catch (TransformException e) {
					IOUtil.pushData(resp, e.getMessage());
					return;
				}
			}
			else{
				newCrsSfc=queryResult;
			}
			FeatureJSON fjson = new FeatureJSON();
			StringWriter writer = new StringWriter();

			fjson.writeFeatureCollection(newCrsSfc, writer);
			IOUtil.pushData(resp, writer.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public SimpleFeatureCollection getLeidianPoint(String geoFilter) throws Exception{
		
		String path=Query.getPath()+"/shp/leidian.shp";
		Map<String, URL> params=new HashMap<String, URL>();
		params.put("url", new File(path).toURL());
		DataStore datastore=DataStoreFinder.getDataStore(params);
		SimpleFeatureSource sf= datastore.getFeatureSource("leidian");
		SimpleFeatureCollection features=null;
		try {
			features = sf.getFeatures(CQL.toFilter("WITHIN(the_geom,"+geoFilter+")"));
		} catch (CQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return features;
	}

}
