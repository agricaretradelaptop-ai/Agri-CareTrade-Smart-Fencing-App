package com.agricaretrade.fencing;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.print.*;
import android.webkit.*;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
 private WebView webView; private final int PAGE_W=595,PAGE_H=842,M=36;

 @Override public void onCreate(Bundle b){
  super.onCreate(b);webView=new WebView(this);setContentView(webView);
  WebSettings s=webView.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(true);s.setAllowContentAccess(true);
  webView.addJavascriptInterface(new AndroidBridge(),"AndroidBridge");webView.setWebChromeClient(new WebChromeClient());
  webView.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return external(r.getUrl());}@SuppressWarnings("deprecation")@Override public boolean shouldOverrideUrlLoading(WebView v,String u){return external(Uri.parse(u));}});
  webView.loadUrl("file:///android_asset/index.html");
 }
 private boolean external(Uri u){String s=u.getScheme()==null?"":u.getScheme();if(s.equals("mailto")||s.equals("tel")||s.equals("http")||s.equals("https")){try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}return true;}return false;}
 private void printPage(){PrintManager pm=(PrintManager)getSystemService(Context.PRINT_SERVICE);pm.print("Agri CareTrade Fencing Quote",webView.createPrintDocumentAdapter("Agri CareTrade Fencing Quote"),new PrintAttributes.Builder().build());}

 public class AndroidBridge{
  @JavascriptInterface public void printPage(){runOnUiThread(()->MainActivity.this.printPage());}
  @JavascriptInterface public String getEmbeddedProducts(){
   try(InputStream in=getAssets().open("products.json")){ return readStream(in); }
   catch(Exception e){ return "[]"; }
  }
  @JavascriptInterface public void submitQuote(String json){runOnUiThread(()->{try{JSONObject q=new JSONObject(json);File pdf=createQuotePdf(q);emailPdf(q,pdf);}catch(Exception e){Toast.makeText(MainActivity.this,"Could not create quote PDF: "+e.getMessage(),Toast.LENGTH_LONG).show();}});}
 }

 private String readStream(InputStream in)throws Exception{if(in==null)return "";try(BufferedReader br=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){StringBuilder sb=new StringBuilder();String line;while((line=br.readLine())!=null)sb.append(line);return sb.toString();}}
 private Paint paint(float size,boolean bold,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTextSize(size);p.setColor(color);p.setTypeface(bold?Typeface.create(Typeface.DEFAULT,Typeface.BOLD):Typeface.DEFAULT);return p;}
 private static class State{PdfDocument doc;PdfDocument.Page page;Canvas c;int y;int pageNo=0;}
 private void newPage(State s,String ref){if(s.page!=null){footer(s);s.doc.finishPage(s.page);}s.pageNo++;s.page=s.doc.startPage(new PdfDocument.PageInfo.Builder(PAGE_W,PAGE_H,s.pageNo).create());s.c=s.page.getCanvas();s.c.drawColor(Color.WHITE);s.y=M;drawHeader(s,ref);}
 private void drawHeader(State s,String ref){Bitmap logo=BitmapFactory.decodeResource(getResources(),R.drawable.agri_logo);if(logo!=null){float maxW=175,maxH=54,scale=Math.min(maxW/logo.getWidth(),maxH/logo.getHeight());RectF dst=new RectF(M,s.y,M+logo.getWidth()*scale,s.y+logo.getHeight()*scale);s.c.drawBitmap(logo,null,dst,null);}Paint title=paint(18,true,Color.rgb(8,53,83));s.c.drawText("Smart Fencing Quote",355,s.y+22,title);Paint sm=paint(8,false,Color.DKGRAY);s.c.drawText("Quote: "+ref,355,s.y+38,sm);s.y+=65;s.c.drawLine(M,s.y,PAGE_W-M,s.y,paint(1,false,Color.rgb(62,138,50)));s.y+=15;}
 private void footer(State s){Paint p=paint(7,false,Color.GRAY);s.c.drawText("Agri CareTrade | 028 9446 2266 | www.agricaretrade.com",M,PAGE_H-20,p);String n="Page "+s.pageNo;s.c.drawText(n,PAGE_W-M-p.measureText(n),PAGE_H-20,p);}
 private void need(State s,int h,String ref){if(s.y+h>PAGE_H-42)newPage(s,ref);}
 private int wrap(State s,String text,float x,float width,Paint p,int gap,String ref){if(text==null)text="";String[] words=text.split("\\s+");String line="";int lines=0;for(String w:words){String test=line.isEmpty()?w:line+" "+w;if(p.measureText(test)>width&&!line.isEmpty()){need(s,gap+3,ref);s.c.drawText(line,x,s.y,p);s.y+=gap;lines++;line=w;}else line=test;}if(!line.isEmpty()){need(s,gap+3,ref);s.c.drawText(line,x,s.y,p);s.y+=gap;lines++;}return lines;}
 private void heading(State s,String text,String ref){need(s,30,ref);s.y+=4;s.c.drawText(text,M,s.y,paint(13,true,Color.rgb(8,53,83)));s.y+=10;s.c.drawLine(M,s.y,PAGE_W-M,s.y,paint(1,false,Color.LTGRAY));s.y+=12;}
 private void field(State s,String label,String value,String ref){need(s,16,ref);Paint a=paint(8,true,Color.DKGRAY),b=paint(9,false,Color.BLACK);s.c.drawText(label,M,s.y,a);wrap(s,value==null||value.isEmpty()?"-":value,M+105,PAGE_W-M-(M+105),b,12,ref);}

 private File createQuotePdf(JSONObject q)throws Exception{
  String ref=q.optString("quoteRef","FQ");PdfDocument doc=new PdfDocument();State s=new State();s.doc=doc;newPage(s,ref);
  String source=q.optString("stockSource","Embedded stock snapshot");
  heading(s,"Stock & Price Source",ref);field(s,"Source",source,ref);
  heading(s,"Customer & Job Details",ref);field(s,"Customer",q.optString("customer"),ref);field(s,"Farm / Business",q.optString("farm"),ref);field(s,"Mobile",q.optString("phone"),ref);field(s,"Email",q.optString("email"),ref);field(s,"Area / Postcode",q.optString("area"),ref);field(s,"Fence length",q.optDouble("length")+" m",ref);field(s,"System",q.optString("system"),ref);field(s,"Livestock",q.optString("stock"),ref);field(s,"Terrain",q.optString("terrain"),ref);field(s,"Post spacing",q.optDouble("spacing")+" m",ref);field(s,"Corners / changes",String.valueOf(q.optInt("corners")),ref);field(s,"Gateways",String.valueOf(q.optInt("gates")),ref);field(s,"Notes",q.optString("notes"),ref);
  heading(s,"All Products Required For This Job",ref);JSONArray items=q.getJSONArray("items");Paint code=paint(8,true,Color.rgb(8,53,83)),normal=paint(8,false,Color.BLACK),small=paint(7,false,Color.DKGRAY);
  for(int i=0;i<items.length();i++){JSONObject it=items.getJSONObject(i);need(s,55,ref);s.c.drawText(it.optString("code"),M,s.y,code);s.c.drawText("Qty "+it.optInt("qty"),460,s.y,code);s.y+=11;wrap(s,it.optString("name"),M,PAGE_W-2*M,normal,10,ref);String meta="Free stock: "+fmtQty(it.optDouble("free"))+"   On POP order: "+fmtQty(it.optDouble("onOrder"))+"   Unit ex VAT: "+gbp(it.optDouble("unitEx"))+"   Unit inc VAT: "+gbp(it.optDouble("unitInc"));wrap(s,meta,M,PAGE_W-2*M,small,9,ref);s.c.drawText("Line inc VAT: "+gbp(it.optDouble("lineInc")),M,s.y,small);s.y+=12;s.c.drawLine(M,s.y,PAGE_W-M,s.y,paint(1,false,Color.rgb(235,239,242)));s.y+=8;}
  heading(s,"Products Needed Ordered",ref);JSONArray order=q.optJSONArray("orderItems");if(order==null||order.length()==0){wrap(s,"No product shortages are shown against the current stock data.",M,PAGE_W-2*M,normal,11,ref);}else{for(int i=0;i<order.length();i++){JSONObject it=order.getJSONObject(i);need(s,58,ref);s.c.drawText(it.optString("code"),M,s.y,code);s.y+=11;wrap(s,it.optString("name"),M,PAGE_W-2*M,normal,10,ref);String l1="Required: "+it.optInt("qty")+"   Free stock: "+fmtQty(it.optDouble("free"))+"   Job shortage: "+fmtQty(it.optDouble("shortage"));wrap(s,l1,M,PAGE_W-2*M,small,9,ref);String l2="Already on POP order: "+fmtQty(it.optDouble("onOrder"))+"   ADDITIONAL TO ORDER: "+fmtQty(it.optDouble("additionalToOrder"));Paint orderPaint=paint(8,true,it.optDouble("additionalToOrder")>0?Color.rgb(166,56,56):Color.rgb(62,110,49));wrap(s,l2,M,PAGE_W-2*M,orderPaint,10,ref);s.y+=6;}}
  heading(s,"Quote Totals",ref);field(s,"Materials ex VAT",gbp(q.optDouble("totalEx")),ref);field(s,"VAT 20%",gbp(q.optDouble("vat")),ref);field(s,"TOTAL INC VAT",gbp(q.optDouble("totalInc")),ref);s.y+=8;
  wrap(s,"Final site specification, price and availability should be confirmed before order. Stock and purchase-order figures are based on the stock snapshot embedded in the app.",M,PAGE_W-2*M,paint(7,false,Color.GRAY),10,ref);
  footer(s);doc.finishPage(s.page);File dir=new File(getCacheDir(),"quotes");if(!dir.exists())dir.mkdirs();String customer=safe(q.optString("customer",q.optString("farm","Customer")));File out=new File(dir,"Agri_Fencing_"+ref+"_"+customer+".pdf");try(FileOutputStream os=new FileOutputStream(out)){doc.writeTo(os);}doc.close();return out;
 }
 private String safe(String s){s=s==null?"Customer":s.trim();if(s.isEmpty())s="Customer";return s.replaceAll("[^A-Za-z0-9_-]+","_");}
 private String gbp(double x){return String.format(Locale.UK,"£%,.2f",x);}private String fmtQty(double x){return Math.abs(x-Math.rint(x))<.001?String.format(Locale.UK,"%.0f",x):String.format(Locale.UK,"%.2f",x);}
 private void emailPdf(JSONObject q,File pdf){Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",pdf);Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_EMAIL,new String[]{"ben.glass@agricaretrade.com"});i.putExtra(Intent.EXTRA_SUBJECT,"Fencing Quote "+q.optString("quoteRef")+" - "+(q.optString("customer").isEmpty()?q.optString("farm","Customer"):q.optString("customer")));i.putExtra(Intent.EXTRA_TEXT,"Agri CareTrade Smart Fencing quote attached.\n\nStock source: "+q.optString("stockSource","Embedded stock snapshot")+"\nTotal inc VAT: "+gbp(q.optDouble("totalInc"))+"\nProducts needing stock attention: "+q.optJSONArray("orderItems").length());i.putExtra(Intent.EXTRA_STREAM,uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("Fencing Quote PDF",uri));startActivity(Intent.createChooser(i,"Email fencing quote to Ben"));}
 @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
}
