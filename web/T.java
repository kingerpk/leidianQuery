/**
 * 
 */
package web;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import util.IOUtil;

import com.vividsolutions.jts.awt.PointShapeFactory.Point;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author lin
 *
 */
public class T {
	
	public T(){}
	

	
	@Test
	public void te() throws Exception{
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		//set the name
		b.setName( "Flag" );

		//add some properties
		b.add( "name", String.class );

		//add a geometry property
		b.setCRS( DefaultGeographicCRS.WGS84 ); // set crs first
		b.add( "location", MultiPolygon.class); // then add geometry

		//build the type
		final SimpleFeatureType TYPE = b.buildFeatureType();
		double DpreKM=1/111.11;
		double KM=1;
		String path="F:/temdata/粤港供水/leida.shp";
		Map<String, URL> params=new HashMap<String, URL>();
		params.put("url", new File(path).toURL());
		DataStore datastore=DataStoreFinder.getDataStore(params);
		SimpleFeatureSource sf= datastore.getFeatureSource("leida");
		SimpleFeatureCollection features=null;
		features = sf.getFeatures();
		SimpleFeatureIterator fi= features.features();
		MultiPolygon mp=null;
		SimpleFeatureBuilder sb=new SimpleFeatureBuilder(TYPE);
		DefaultFeatureCollection dfc=new DefaultFeatureCollection("my", TYPE);
		GeometryFactory factory = org.geotools.geometry.jts.FactoryFinder.getGeometryFactory(null);
		List<Geometry> geos=new ArrayList<Geometry>();
		while(fi.hasNext()){
			
			SimpleFeature sff=fi.next();
			MultiLineString ml=(MultiLineString)sff.getDefaultGeometry();
			com.vividsolutions.jts.geom.Geometry geo= ml.buffer(KM*DpreKM);
			geos.add(geo);
			sb.add("fuuu");
			sb.add(geo);
			SimpleFeature outputf=sb.buildFeature(null);
			dfc.add(outputf);
		}
		
		GeometryCollection geocollection=(GeometryCollection)factory.buildGeometry(geos);
		
		SimpleFeatureTypeBuilder b1 = new SimpleFeatureTypeBuilder();
		//set the name
		b1.setName( "Flag" );

		//add some properties
		b1.add( "name", String.class );

		//add a geometry property
		b1.setCRS( DefaultGeographicCRS.WGS84 ); // set crs first
		b1.add( "location", Polygon.class); // then add geometry

		//build the type
		final SimpleFeatureType TYPE1 = b1.buildFeatureType();
		SimpleFeatureBuilder sb1=new SimpleFeatureBuilder(TYPE1);
		sb1.add("fk");
		sb1.add(geocollection.buffer(DpreKM*KM));
		SimpleFeature fallS= sb1.buildFeature("");
		
		FeatureJSON fjson = new FeatureJSON();
		StringWriter writer = new StringWriter();

		fjson.writeFeature(fallS, writer);

		String json = writer.toString();
		System.out.println(json);
	}
	
}
