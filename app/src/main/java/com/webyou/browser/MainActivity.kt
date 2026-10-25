package com.webyou.browser

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import java.net.URLEncoder

private const val HOME="https://www.google.com"
private val AD_HOSTS=setOf("doubleclick.net","googlesyndication.com","googleadservices.com","adservice.google.com","adnxs.com","taboola.com","outbrain.com","criteo.com","rubiconproject.com","pubmatic.com","scorecardresearch.com")

enum class AppTheme{SYSTEM,LIGHT,DARK,AMOLED}
data class Tab(val id:Int,var url:String=HOME,var title:String="Google",var incognito:Boolean=false)
class State{val tabs=mutableStateListOf(Tab(1));var active by mutableIntStateOf(1);var screen by mutableStateOf("home");var theme by mutableStateOf(AppTheme.SYSTEM);var accent by mutableIntStateOf(0);var adblock by mutableStateOf(true);var blocked by mutableIntStateOf(0);var waveDepth by mutableFloatStateOf(.08f);var waveCount by mutableIntStateOf(8);fun tab()=tabs.first{it.id==active};fun add(incognito:Boolean=false){val id=(tabs.maxOfOrNull{it.id}?:0)+1;tabs.add(Tab(id,title=if(incognito)"Инкогнито" else "Новая вкладка",incognito=incognito));active=id;screen="home"};fun close(id:Int){if(tabs.size==1){tabs[0]=Tab(1);active=1}else{val i=tabs.indexOfFirst{it.id==id};tabs.removeAll{it.id==id};if(active==id)active=tabs[i.coerceAtMost(tabs.lastIndex)].id}}}

class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);WindowCompat.setDecorFitsSystemWindows(window,false);setContent{App()}}}

@Composable fun App(){val s=remember{State()};val dark=when(s.theme){AppTheme.DARK,AppTheme.AMOLED->true;AppTheme.LIGHT->false;AppTheme.SYSTEM->androidx.compose.foundation.isSystemInDarkTheme()};val p=listOf(Color(0xFF7656D6),Color(0xFF3C6EDB),Color(0xFF008A78),Color(0xFFC85C8E))[s.accent];MaterialTheme(colorScheme=if(dark)darkColorScheme(primary=listOf(Color(0xFFC9B7FF),Color(0xFFA9C5FF),Color(0xFF8BE0D0),Color(0xFFFFB1D0))[s.accent],background=if(s.theme==AppTheme.AMOLED)Color.Black else Color(0xFF191A1A),surface=if(s.theme==AppTheme.AMOLED)Color.Black else Color(0xFF222424),surfaceContainer=if(s.theme==AppTheme.AMOLED)Color(0xFF111212) else Color(0xFF292B2B))else lightColorScheme(primary=p,background=Color(0xFFF9F7FF),surface=Color(0xFFFCF9FF),surfaceContainer=Color(0xFFF1EDF7))){AnimatedContent(s.screen,label="screen"){when(it){"home"->Home(s);"browser"->Browser(s);"tabs"->Tabs(s);"menu"->Menu(s);"settings"->Settings(s);"customize"->Customize(s);"adblock"->Adblock(s);"privacy"->Privacy(s);"translate"->Translate(s);else->Home(s)}}}}

@Composable fun Background(s:State){Canvas(Modifier.fillMaxSize()){val dark=MaterialTheme.colorScheme.background.luminance()<.5f;drawRect(if(dark)Color(0xFF161717) else Color(0xFFF9F7FF));val c=if(dark)Color(0xFF312842) else Color(0xFFECE5FF);val h=size.height*s.waveDepth;val p=Path().apply{moveTo(0f,0f);cubicTo(size.width*.25f,h,size.width*.45f,-h*.4f,size.width*.65f,h*.6f);cubicTo(size.width*.82f,h,size.width*.92f,h*.2f,size.width,h*.5f);lineTo(size.width,0f);close()};drawPath(p,c)}}

@Composable fun Pill(t:String,icon:androidx.compose.ui.graphics.vector.ImageVector?=null,m:Modifier=Modifier,on:()->Unit={}){val sh=RoundedCornerShape(50.dp,32.dp,42.dp,18.dp);Surface(m.clip(sh).clickable{on()},shape=sh,color=MaterialTheme.colorScheme.surfaceContainer){Row(Modifier.padding(horizontal=15.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically){icon?.let{Icon(it,null,Modifier.size(19.dp));Spacer(Modifier.width(8.dp))};Text(t,fontWeight=FontWeight.Medium)}}}

@Composable fun SearchBox(v:String,set:(String)->Unit,go:()->Unit){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp,18.dp,30.dp,24.dp),tonalElevation=2.dp){Row(Modifier.padding(8.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Search,null,tint=MaterialTheme.colorScheme.primary);Box(Modifier.weight(1f).padding(12.dp)){if(v.isEmpty())Text("Введите запрос или URL",color=MaterialTheme.colorScheme.onSurfaceVariant);BasicTextField(v,set,singleLine=true,modifier=Modifier.fillMaxWidth())};IconButton(go){Icon(Icons.Rounded.OpenInNew,null)}}}}

@Composable fun Home(s:State){var q by remember{mutableStateOf("")};Box(Modifier.fillMaxSize()){Background(s);Column(Modifier.fillMaxSize().safeDrawingPadding().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Pill("Настроить",Icons.Rounded.Tune){s.screen="customize"}};Spacer(Modifier.height(45.dp));Surface(Modifier.size(84.dp),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Icon(Icons.Rounded.Wifi,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary)}};Text("WebYou",fontSize=34.sp,fontWeight=FontWeight.Bold);Text("Быстрый. Приватный. Твой.",color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(28.dp));SearchBox(q,{q=it}){open(s,q)};Spacer(Modifier.height(16.dp));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){Pill("Google",Icons.Rounded.Search){open(s,HOME)};Pill("YouTube",Icons.Rounded.OpenInNew){open(s,"https://youtube.com")};Pill("Wikipedia",Icons.Rounded.Language){open(s,"https://wikipedia.org")}};Spacer(Modifier.height(18.dp));Pill("Новая вкладка",Icons.Rounded.Add,Modifier.fillMaxWidth()){s.add()};Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Pill("Вкладки",Icons.Rounded.Visibility,Modifier.weight(1f)){s.screen="tabs"};Pill("Приватность",Icons.Rounded.PrivacyTip,Modifier.weight(1f)){s.screen="privacy"}};Spacer(Modifier.height(10.dp));Pill("Перевод страницы",Icons.Rounded.Translate,Modifier.fillMaxWidth()){s.screen="translate"};Spacer(Modifier.weight(1f));Nav(s)}}

@SuppressLint("SetJavaScriptEnabled")
@Composable fun Browser(s:State){val t=s.tab();var address by remember(t.id,t.url){mutableStateOf(t.url)};Column(Modifier.fillMaxSize().safeDrawingPadding()){Row(Modifier.fillMaxWidth().padding(8.dp),verticalAlignment=Alignment.CenterVertically){IconButton({s.screen="home"}){Icon(Icons.AutoMirrored.Rounded.ArrowBack,null)};Surface(Modifier.weight(1f),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surfaceContainer){BasicTextField(address,{address=it},singleLine=true,modifier=Modifier.fillMaxWidth().padding(12.dp))};IconButton({open(s,address)}){Icon(Icons.Rounded.Search,null)};IconButton({s.screen="menu"}){Icon(Icons.Rounded.MoreVert,null)}};AndroidView(factory={c->WebView(c).apply{settings.javaScriptEnabled=true;settings.domStorageEnabled=true;webViewClient=object:WebViewClient(){override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse?{if(s.adblock&&AD_HOSTS.any{r.url.host?.endsWith(it)==true}){s.blocked++;return WebResourceResponse("text/plain","utf-8",null)};return null};override fun onPageFinished(v:WebView,u:String){t.url=u;t.title=v.title?:u}};loadUrl(t.url)}},modifier=Modifier.weight(1f));Nav(s)}}

@Composable fun Nav(s:State){Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(34.dp,22.dp,38.dp,26.dp),tonalElevation=4.dp){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){IconButton({s.screen="home"}){Icon(Icons.Rounded.Home,null)};IconButton({s.screen="tabs"}){Icon(Icons.Rounded.Visibility,null)};IconButton({s.screen="menu"}){Icon(Icons.Rounded.MoreVert,null)};IconButton({s.add()}){Icon(Icons.Rounded.Add,null)}}}}

@Composable fun Tabs(s:State){Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)){Top("Вкладки",s);s.tabs.forEach{t->Surface(Modifier.fillMaxWidth().padding(bottom=10.dp).clickable{s.active=t.id;s.screen="browser"},shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surfaceContainer){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(t.title,fontWeight=FontWeight.Bold);Text(t.url,maxLines=1,overflow=TextOverflow.Ellipsis,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}IconButton({s.close(t.id)}){Icon(Icons.Rounded.Close,null)}}}};Pill("Инкогнито",Icons.Rounded.Incognito,Modifier.fillMaxWidth()){s.add(true)}}

@Composable fun Menu(s:State){Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)){Top("Меню",s);listOf("Новая вкладка" to Icons.Rounded.Add,"Инкогнито" to Icons.Rounded.Incognito,"Настройки" to Icons.Rounded.Settings,"Блокировка рекламы" to Icons.Rounded.Shield,"Перевод" to Icons.Rounded.Translate,"Приватность" to Icons.Rounded.PrivacyTip).forEach{(x,i)->Pill(x,i,Modifier.fillMaxWidth()){when(x){"Новая вкладка"->s.add();"Инкогнито"->s.add(true);"Настройки"->s.screen="settings";"Блокировка рекламы"->s.screen="adblock";"Перевод"->s.screen="translate";else->s.screen="privacy"}};Spacer(Modifier.height(9.dp))}}}

@Composable fun Settings(s:State){Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)){Top("Настройки",s);Pill("Внешний вид",Icons.Rounded.Palette,Modifier.fillMaxWidth()){s.screen="customize"};Spacer(Modifier.height(10.dp));Pill("Блокировка рекламы",Icons.Rounded.Shield,Modifier.fillMaxWidth()){s.screen="adblock"};Spacer(Modifier.height(10.dp));Pill("Конфиденциальность",Icons.Rounded.PrivacyTip,Modifier.fillMaxWidth()){s.screen="privacy"}}}

@Composable fun Customize(s:State){Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Top("Внешний вид",s);Text("Тема",fontWeight=FontWeight.Bold);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)){AppTheme.values().forEach{Pill(it.name,selected=it==s.theme){s.theme=it}}};Text("Акцент",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){repeat(4){i->Surface(Modifier.size(45.dp).clickable{s.accent=i},shape=RoundedCornerShape(16.dp),color=listOf(Color(0xFF7656D6),Color(0xFF3C6EDB),Color(0xFF008A78),Color(0xFFC85C8E))[i]){}}};Text("Глубина волн: ${(s.waveDepth*100).toInt()}%");Slider(s.waveDepth,{s.waveDepth=it},valueRange=.02f..0.18f);Text("Количество волн: ${s.waveCount}");Slider(s.waveCount.toFloat(),{s.waveCount=it.toInt()},valueRange=2f..12f,steps=9)}}

@Composable fun Adblock(s:State){Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Top("Блокировка рекламы",s);Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(28.dp),color=MaterialTheme.colorScheme.surfaceContainer){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text("Чистый интернет",fontSize=22.sp,fontWeight=FontWeight.Bold);Row(verticalAlignment=Alignment.CenterVertically){Text("Блокировать рекламу",Modifier.weight(1f));Switch(s.adblock,{s.adblock=it})};Text("Заблокировано запросов: ${s.blocked}")}}}}

@Composable fun Privacy(s:State){Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Top("Конфиденциальность",s);Pill("Блокировать трекеры",Icons.Rounded.PrivacyTip,Modifier.fillMaxWidth());Pill("Безопасный просмотр",Icons.Rounded.Security,Modifier.fillMaxWidth());Pill("Инкогнито",Icons.Rounded.Incognito,Modifier.fillMaxWidth()){s.add(true)}}}

@Composable fun Translate(s:State){Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Top("Перевод страницы",s);Text(s.tab().url,maxLines=2,overflow=TextOverflow.Ellipsis);Pill("Перевести через Google",Icons.Rounded.Translate,Modifier.fillMaxWidth()){open(s,"https://translate.google.com/translate?sl=auto&tl=ru&u="+URLEncoder.encode(s.tab().url,"UTF-8"))}}}

@Composable fun Top(t:String,s:State){Row(Modifier.fillMaxWidth().padding(bottom=14.dp),verticalAlignment=Alignment.CenterVertically){IconButton({s.screen=if(s.tab().url==HOME)"home" else "browser"}){Icon(Icons.AutoMirrored.Rounded.ArrowBack,null)};Text(t,fontSize=25.sp,fontWeight=FontWeight.Bold)}}
private fun open(s:State,input:String){val q=input.trim();if(q.isEmpty())return;s.tab().url=if(q.startsWith("http://")||q.startsWith("https://"))q else if(q.contains(".")&&!q.contains(" "))"https://$q" else "https://www.google.com/search?q="+URLEncoder.encode(q,"UTF-8");s.screen="browser"}
