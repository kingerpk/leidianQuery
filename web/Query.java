package web;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.collection.CollectionFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import util.IOUtil;

import com.vividsolutions.jts.awt.PointShapeFactory.Point;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Polygon;

public class Query extends HttpServlet {

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	public static String getPath() throws IOException
	{
		//获取存放Properties文件的目录
		String path=null;
		
			path = Query.class.getProtectionDomain().getCodeSource().getLocation().getPath();//PropertiesUnit.class.getResource("/").toURI().getPath();
		
			return 	path.substring(0, path.lastIndexOf("classes"));
	}
	
	public static HashMap<String,CoordinateReferenceSystem> CrsMap=new HashMap<String, CoordinateReferenceSystem>(); 
	public void t() throws NoSuchAuthorityCodeException, FactoryException{
		 CRS.decode("EPSG:4326");
	}
	
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
		
		String datacrsCode=req.getParameter("datacrs");
		String targetcrsCode=req.getParameter("targetcrs");
		CoordinateReferenceSystem datacrs=CrsMap.get(datacrsCode);
		CoordinateReferenceSystem targetcrs=CrsMap.get(targetcrsCode);
		if(datacrs==null){
			try {
				datacrs=CRS.decode(datacrsCode);
				CrsMap.put(datacrsCode, datacrs);
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
				CrsMap.put(targetcrsCode, targetcrs);
			} catch (NoSuchAuthorityCodeException e) {
				IOUtil.pushData(resp, e.getMessage());
				return;
			} catch (FactoryException e) {
				IOUtil.pushData(resp, e.getMessage());
				return;
			}
		}
		
		double DpreKM=1/111.11;
		double KM=Double.parseDouble(req.getParameter("buffer"));
		String path=getPath()+"/shp/leida.shp";
		Map<String, URL> params=new HashMap<String, URL>();
		params.put("url", new File(path).toURL());
		DataStore datastore=DataStoreFinder.getDataStore(params);
		SimpleFeatureSource sf= datastore.getFeatureSource("leida");
		SimpleFeatureCollection features=null;
		features = sf.getFeatures();
		SimpleFeatureIterator fi= features.features();
		GeometryFactory factory = org.geotools.geometry.jts.FactoryFinder.getGeometryFactory(null);
		List<Geometry> geos=new ArrayList<Geometry>();
		while(fi.hasNext()){
			
			SimpleFeature sff=fi.next();
			MultiLineString ml=(MultiLineString)sff.getDefaultGeometry();
			com.vividsolutions.jts.geom.Geometry geo= ml.buffer(KM*DpreKM);
			geos.add((Geometry) geo);
		}
		
		GeometryCollection geocollection=(GeometryCollection)factory.buildGeometry(geos);
		Geometry buffGeo=geocollection.buffer(KM*DpreKM);
		Geometry ResultBuffGeo=null;
		SimpleFeatureCollection queryResult=null;
		SimpleFeatureCollection newCrsSfc= FeatureCollections.newCollection();
		try {
			queryResult=getLeidianPoint(buffGeo.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
				ResultBuffGeo= JTS.transform(buffGeo, transform);
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
			ResultBuffGeo=buffGeo;
			newCrsSfc=queryResult;
		}
		
		SimpleFeature fallS= builderFeature(ResultBuffGeo, "polygon");
		
		FeatureJSON fjson = new FeatureJSON();
		StringWriter writer = new StringWriter();

		fjson.writeFeature(fallS, writer);

		String bufferJson = writer.toString();
		
		 writer = new StringWriter();
		fjson.writeFeatureCollection(newCrsSfc, writer);
		String queryResultJson=writer.toString();
		IOUtil.pushData(resp, bufferJson+"@everylightszfilter@"+queryResultJson);
	}
	
	public SimpleFeature CrsTransform(CoordinateReferenceSystem sourceCRS,CoordinateReferenceSystem targetCRS,SimpleFeature sf) throws FactoryException, MismatchedDimensionException, TransformException{
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
		Geometry geo2= JTS.transform((Geometry) sf.getDefaultGeometry(), transform);
		sf.setDefaultGeometry(geo2);
		return sf;
	}
	
	public SimpleFeature builderFeature(com.vividsolutions.jts.geom.Geometry geometry,String type){
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		//set the name
		b.setName( "Flag" );

		//add a geometry property
		b.setCRS( DefaultGeographicCRS.WGS84 ); // set crs first
		if(type.equals("polygon")){
			b.add( "location", Polygon.class); // then add geometry
		}
		else if(type.equals("point")){
			b.add( "location", Point.class); // then add geometry
		}
		//build the type
		final SimpleFeatureType TYPE = b.buildFeatureType();
		SimpleFeatureBuilder sb=new SimpleFeatureBuilder(TYPE);
		
		sb.add(geometry);
		return sb.buildFeature("");
	}
	
	public SimpleFeatureCollection getLeidianPoint(String geoFilter) throws Exception{
		
		String path=getPath()+"/shp/leidian.shp";
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
