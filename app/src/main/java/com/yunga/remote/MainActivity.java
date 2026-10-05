package com.yunga.remote;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.ConsumerIrManager;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private static final int REQ_BT = 701;
    private static final int IR_FREQ = 38000;
    private static final int NEC_ADDR = 0x01;

    private static final int CMD_POWER=0x40, CMD_UP=0x16, CMD_DOWN=0x1A, CMD_LEFT=0x51,
            CMD_RIGHT=0x50, CMD_OK=0x13, CMD_HOME=0x11, CMD_BACK=0x19, CMD_MENU=0x4C,
            CMD_VOL_DOWN=0x10, CMD_VOL_UP=0x18, CMD_MUTE=0x41, CMD_PLAY=0x5A, CMD_MOUSE=0x00;

    private static final int BG=Color.rgb(8,17,31), CARD=Color.rgb(17,27,45),
            BTN=Color.rgb(31,45,65), ACCENT=Color.rgb(34,211,238),
            TEXT=Color.rgb(241,245,249), MUTED=Color.rgb(148,163,184),
            RED=Color.rgb(239,68,68), GREEN=Color.rgb(52,211,153);

    private ConsumerIrManager ir;
    private TextView irStatus, btStatus;
    private BluetoothAdapter bt;
    private BluetoothHidDevice hid;
    private BluetoothDevice host;
    private boolean hidRegistered=false;
    private Spinner pairedSpinner;
    private final ArrayList<BluetoothDevice> pairedDevices=new ArrayList<>();
    private final ArrayList<String> pairedNames=new ArrayList<>();
    private final ExecutorService executor=Executors.newSingleThreadExecutor();

    private final byte[] keyboardDescriptor = new byte[] {
            0x05,0x01,0x09,0x06,(byte)0xA1,0x01,0x05,0x07,0x19,(byte)0xE0,0x29,(byte)0xE7,
            0x15,0x00,0x25,0x01,0x75,0x01,(byte)0x95,0x08,(byte)0x81,0x02,
            (byte)0x95,0x01,0x75,0x08,(byte)0x81,0x01,(byte)0x95,0x05,0x75,0x01,
            0x05,0x08,0x19,0x01,0x29,0x05,(byte)0x91,0x02,(byte)0x95,0x01,0x75,0x03,
            (byte)0x91,0x01,(byte)0x95,0x06,0x75,0x08,0x15,0x00,0x25,0x65,0x05,0x07,
            0x19,0x00,0x29,0x65,(byte)0x81,0x00,(byte)0xC0
    };

    private final BluetoothHidDevice.Callback hidCallback=new BluetoothHidDevice.Callback(){
        @Override public void onAppStatusChanged(BluetoothDevice d, boolean registered){
            hidRegistered=registered;
            if(d!=null) host=d;
            runOnUiThread(()->{ updateBtStatus(); refreshPaired(); });
        }
        @Override public void onConnectionStateChanged(BluetoothDevice d,int state){
            if(state==BluetoothProfile.STATE_CONNECTED) host=d;
            else if(host!=null && host.equals(d) && state==BluetoothProfile.STATE_DISCONNECTED) host=null;
            runOnUiThread(()->updateBtStatus());
        }
    };

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        ir=(ConsumerIrManager)getSystemService(Context.CONSUMER_IR_SERVICE);
        setupBluetooth();
        setContentView(buildUi());
        updateIrStatus();
        requestBtPermissions();
    }

    private void setupBluetooth(){
        BluetoothManager m=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        bt=m==null?null:m.getAdapter();
        if(bt!=null && Build.VERSION.SDK_INT>=28){
            bt.getProfileProxy(this,new BluetoothProfile.ServiceListener(){
                @Override public void onServiceConnected(int p,BluetoothProfile proxy){
                    if(p==BluetoothProfile.HID_DEVICE){ hid=(BluetoothHidDevice)proxy; updateBtStatus(); }
                }
                @Override public void onServiceDisconnected(int p){
                    if(p==BluetoothProfile.HID_DEVICE){ hid=null; hidRegistered=false; host=null; updateBtStatus(); }
                }
            },BluetoothProfile.HID_DEVICE);
        }
    }

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root=col();
        root.setPadding(dp(18),dp(16),dp(18),dp(32));
        scroll.addView(root);

        root.addView(txt("Yunga Remote",30,TEXT,true));
        TextView sub=txt("Control X96 Max+ por infrarrojo + teclado Bluetooth",14,MUTED,false);
        sub.setPadding(0,dp(4),0,dp(14)); root.addView(sub);

        LinearLayout statuses=row();
        irStatus=pill("IR: comprobando…");
        btStatus=pill("BT: comprobando…");
        statuses.addView(irStatus,new LinearLayout.LayoutParams(0,dp(40),1));
        statuses.addView(space(dp(8),1));
        statuses.addView(btStatus,new LinearLayout.LayoutParams(0,dp(40),1));
        root.addView(statuses);

        root.addView(section("MANDO"));
        LinearLayout card=card(); root.addView(card);

        LinearLayout powerRow=centerRow();
        powerRow.addView(remoteButton("⏻",RED,CMD_POWER,28),square(72)); card.addView(powerRow);

        LinearLayout up=centerRow(); up.setPadding(0,dp(10),0,0);
        up.addView(remoteButton("▲",BTN,CMD_UP,28),square(68)); card.addView(up);

        LinearLayout nav=centerRow(); nav.setPadding(0,dp(6),0,dp(6));
        nav.addView(remoteButton("◀",BTN,CMD_LEFT,28),square(68));
        nav.addView(space(dp(12),1));
        nav.addView(remoteButton("OK",ACCENT,CMD_OK,18),square(76));
        nav.addView(space(dp(12),1));
        nav.addView(remoteButton("▶",BTN,CMD_RIGHT,28),square(68));
        card.addView(nav);

        LinearLayout down=centerRow();
        down.addView(remoteButton("▼",BTN,CMD_DOWN,28),square(68)); card.addView(down);

        card.addView(buttonRow(new String[]{"⌂\nInicio","↩\nAtrás","☰\nMenú"},
                new int[]{CMD_HOME,CMD_BACK,CMD_MENU}));
        card.addView(buttonRow(new String[]{"−\nVol","🔇","+\nVol"},
                new int[]{CMD_VOL_DOWN,CMD_MUTE,CMD_VOL_UP}));

        LinearLayout media=row(); media.setPadding(0,dp(8),0,0);
        media.addView(remoteButton("▶❚❚  Reproducir / Pausa",BTN,CMD_PLAY,14),weight(58));
        media.addView(space(dp(8),1));
        media.addView(remoteButton("🖱  Mouse",BTN,CMD_MOUSE,14),weight(58));
        card.addView(media);

        root.addView(section("TECLADO BLUETOOTH"));
        LinearLayout kcard=card(); root.addView(kcard);
        TextView intro=txt("Sin instalar nada en el TV Stick: el celular intenta funcionar como teclado Bluetooth HID.",14,MUTED,false);
        kcard.addView(intro);

        Button activate=action("1. Activar teclado Bluetooth",ACCENT,BG);
        activate.setOnClickListener(v->registerKeyboard()); kcard.addView(top(activate,14));

        Button discover=action("2. Hacer visible el celular (5 min)",BTN,TEXT);
        discover.setOnClickListener(v->makeDiscoverable()); kcard.addView(top(discover,8));

        Button settings=action("Abrir Bluetooth / Emparejamiento",BTN,TEXT);
        settings.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        kcard.addView(top(settings,8));

        TextView hint=txt("En el TV Stick: Ajustes → Bluetooth/Accesorios → Añadir dispositivo. Selecciona tu Redmi Note 12.",13,MUTED,false);
        hint.setPadding(0,dp(12),0,dp(8)); kcard.addView(hint);

        pairedSpinner=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,pairedNames){
            @Override public View getView(int p,View c,ViewGroup parent){
                TextView t=(TextView)super.getView(p,c,parent);
                t.setTextColor(TEXT); t.setTextSize(14); t.setPadding(dp(12),0,dp(12),0); return t;
            }
        };
        pairedSpinner.setAdapter(adapter);
        pairedSpinner.setBackground(round(BTN,14));
        kcard.addView(pairedSpinner,new LinearLayout.LayoutParams(-1,dp(54)));

        Button refresh=action("Actualizar dispositivos emparejados",BTN,TEXT);
        refresh.setOnClickListener(v->refreshPaired()); kcard.addView(top(refresh,8));

        Button connect=action("3. Conectar como teclado",GREEN,BG);
        connect.setOnClickListener(v->connectHost()); kcard.addView(top(connect,8));

        TextView lbl=txt("Escribir en el TV Stick",16,TEXT,true);
        lbl.setPadding(0,dp(18),0,dp(8)); kcard.addView(lbl);

        EditText input=new EditText(this);
        input.setHint("Escribe aquí…"); input.setHintTextColor(MUTED); input.setTextColor(TEXT);
        input.setTextSize(16); input.setMinLines(2); input.setMaxLines(4);
        input.setPadding(dp(14),dp(12),dp(14),dp(12)); input.setBackground(round(Color.rgb(12,23,39),14));
        kcard.addView(input,new LinearLayout.LayoutParams(-1,-2));

        Button send=action("⌨  Enviar texto",ACCENT,BG);
        send.setOnClickListener(v->{ String s=input.getText().toString(); if(s.isEmpty()) toast("Escribe algo primero"); else { sendText(s); input.setText(""); }});
        kcard.addView(top(send,8));

        LinearLayout special=row(); special.setPadding(0,dp(8),0,0);
        Button del=action("⌫ Borrar",BTN,TEXT); del.setOnClickListener(v->sendSpecial((byte)0x2A));
        Button enter=action("↵ Enter",BTN,TEXT); enter.setOnClickListener(v->sendSpecial((byte)0x28));
        special.addView(del,weight(54)); special.addView(space(dp(8),1)); special.addView(enter,weight(54)); kcard.addView(special);

        TextView note=txt("El teclado Bluetooth depende del soporte HID del Redmi y del TV Stick. El control infrarrojo funciona de forma independiente.",12,MUTED,false);
        note.setPadding(0,dp(14),0,0); kcard.addView(note);

        refreshPaired();
        return scroll;
    }

    private LinearLayout buttonRow(String[] labels,int[] cmds){
        LinearLayout r=row(); r.setPadding(0,dp(8),0,0);
        for(int i=0;i<labels.length;i++){
            if(i>0) r.addView(space(dp(8),1));
            r.addView(remoteButton(labels[i],BTN,cmds[i],14),weight(64));
        }
        return r;
    }

    private void transmit(int command){
        if(ir==null || !ir.hasIrEmitter()){ toast("No se detecta emisor infrarrojo"); return; }
        try{ ir.transmit(IR_FREQ,nec(NEC_ADDR,command)); }
        catch(Exception e){ toast("No se pudo enviar la señal IR"); }
    }

    private int[] nec(int address,int command){
        int[] bytes={address&255,(~address)&255,command&255,(~command)&255};
        int[] p=new int[67]; int x=0; p[x++]=9000; p[x++]=4500;
        for(int value:bytes) for(int bit=0;bit<8;bit++){ p[x++]=560; p[x++]=((value>>bit)&1)==1?1690:560; }
        p[x]=560; return p;
    }

    private void registerKeyboard(){
        if(Build.VERSION.SDK_INT<28){ toast("Requiere Android 9 o superior"); return; }
        if(!hasBtPerm()){ requestBtPermissions(); return; }
        if(bt==null){ toast("Bluetooth no disponible"); return; }
        if(!bt.isEnabled()){ startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return; }
        if(hid==null){ toast("Perfil HID no disponible todavía"); return; }
        if(hidRegistered){ toast("El teclado Bluetooth ya está activo"); return; }
        BluetoothHidDeviceAppSdpSettings sdp=new BluetoothHidDeviceAppSdpSettings(
                "Yunga Remote","Teclado para TV Stick","Yunga",(byte)0x40,keyboardDescriptor);
        boolean ok=hid.registerApp(sdp,null,null,getMainExecutor(),hidCallback);
        toast(ok?"Activando teclado Bluetooth…":"Android rechazó la activación HID");
    }

    private void makeDiscoverable(){
        if(bt==null){ toast("Bluetooth no disponible"); return; }
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)!=PackageManager.PERMISSION_GRANTED){ requestBtPermissions(); return; }
        Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300); startActivity(i);
    }

    private void refreshPaired(){
        if(pairedSpinner==null || bt==null || !hasBtPerm()){ updateBtStatus(); return; }
        pairedDevices.clear(); pairedNames.clear();
        try{
            ArrayList<BluetoothDevice> list=new ArrayList<>(bt.getBondedDevices());
            Collections.sort(list,(a,b)->safeName(a).compareToIgnoreCase(safeName(b)));
            for(BluetoothDevice d:list){ pairedDevices.add(d); pairedNames.add(safeName(d)); }
        }catch(SecurityException e){ requestBtPermissions(); }
        if(pairedNames.isEmpty()) pairedNames.add("Sin dispositivos emparejados");
        ((ArrayAdapter<?>)pairedSpinner.getAdapter()).notifyDataSetChanged();
    }

    private void connectHost(){
        if(!hidRegistered){ toast("Primero activa el teclado Bluetooth"); return; }
        if(hid==null || pairedDevices.isEmpty()){ toast("Empareja primero el TV Stick"); return; }
        int p=pairedSpinner.getSelectedItemPosition(); if(p<0 || p>=pairedDevices.size()) p=0;
        BluetoothDevice d=pairedDevices.get(p);
        try{ toast(hid.connect(d)?"Conectando con "+safeName(d)+"…":"No se pudo iniciar la conexión"); }
        catch(SecurityException e){ requestBtPermissions(); }
    }

    private void sendText(String s){
        if(!keyboardReady()) return;
        executor.execute(()->{
            for(char c:s.toCharArray()){
                HidKey k=map(c); if(k!=null) sendReport(k.mod,k.code);
            }
        });
    }

    private void sendSpecial(byte code){ if(keyboardReady()) executor.execute(()->sendReport((byte)0,code)); }

    private boolean keyboardReady(){
        if(!hidRegistered || hid==null || host==null){ toast("Conecta primero el TV Stick como teclado Bluetooth"); return false; }
        return true;
    }

    private void sendReport(byte mod,byte code){
        try{
            hid.sendReport(host,0,new byte[]{mod,0,code,0,0,0,0,0});
            Thread.sleep(24);
            hid.sendReport(host,0,new byte[]{0,0,0,0,0,0,0,0});
            Thread.sleep(18);
        }catch(Exception ignored){}
    }

    private HidKey map(char c){
        if(c>='a'&&c<='z') return new HidKey((byte)0,(byte)(0x04+c-'a'));
        if(c>='A'&&c<='Z') return new HidKey((byte)0x02,(byte)(0x04+c-'A'));
        if(c>='1'&&c<='9') return new HidKey((byte)0,(byte)(0x1E+c-'1'));
        if(c=='0') return new HidKey((byte)0,(byte)0x27);
        switch(c){
            case '\n': return new HidKey((byte)0,(byte)0x28);
            case ' ': return new HidKey((byte)0,(byte)0x2C);
            case '-': return new HidKey((byte)0,(byte)0x2D);
            case '_': return new HidKey((byte)0x02,(byte)0x2D);
            case '.': return new HidKey((byte)0,(byte)0x37);
            case ',': return new HidKey((byte)0,(byte)0x36);
            case '/': return new HidKey((byte)0,(byte)0x38);
            case '?': return new HidKey((byte)0x02,(byte)0x38);
            case '!': return new HidKey((byte)0x02,(byte)0x1E);
            case '@': return new HidKey((byte)0x02,(byte)0x1F);
            default: return null;
        }
    }

    private boolean hasBtPerm(){ return Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED; }

    private void requestBtPermissions(){
        if(Build.VERSION.SDK_INT>=31){
            ArrayList<String> m=new ArrayList<>();
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) m.add(Manifest.permission.BLUETOOTH_CONNECT);
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)!=PackageManager.PERMISSION_GRANTED) m.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            if(!m.isEmpty()) requestPermissions(m.toArray(new String[0]),REQ_BT);
        }
        updateBtStatus();
    }

    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){
        super.onRequestPermissionsResult(r,p,g);
        if(r==REQ_BT){ updateBtStatus(); refreshPaired(); }
    }

    private void updateIrStatus(){
        boolean ok=ir!=null && ir.hasIrEmitter();
        if(irStatus!=null){ irStatus.setText(ok?"● IR listo":"● Sin emisor IR"); irStatus.setTextColor(ok?GREEN:RED); }
    }

    private void updateBtStatus(){
        if(btStatus==null) return;
        if(bt==null){ btStatus.setText("● Sin Bluetooth"); btStatus.setTextColor(RED); return; }
        if(!hasBtPerm()){ btStatus.setText("● Permiso BT"); btStatus.setTextColor(Color.rgb(251,191,36)); return; }
        if(host!=null){ btStatus.setText("● Teclado: "+safeName(host)); btStatus.setTextColor(GREEN); }
        else if(hidRegistered){ btStatus.setText("● Teclado listo"); btStatus.setTextColor(ACCENT); }
        else if(hid!=null){ btStatus.setText("● BT disponible"); btStatus.setTextColor(MUTED); }
        else { btStatus.setText("● HID no disponible"); btStatus.setTextColor(Color.rgb(251,191,36)); }
    }

    private String safeName(BluetoothDevice d){
        if(d==null) return "Dispositivo";
        try{ String n=d.getName(); return n==null||n.trim().isEmpty()?d.getAddress():n; }
        catch(SecurityException e){ return "Dispositivo Bluetooth"; }
    }

    @Override protected void onDestroy(){
        super.onDestroy(); executor.shutdownNow();
        if(bt!=null && hid!=null) try{ bt.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid); }catch(Exception ignored){}
    }

    private LinearLayout col(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row(){ LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    private LinearLayout centerRow(){ LinearLayout l=row(); l.setGravity(Gravity.CENTER); return l; }
    private LinearLayout card(){ LinearLayout l=col(); l.setPadding(dp(14),dp(16),dp(14),dp(16)); l.setBackground(round(CARD,22)); return l; }

    private TextView txt(String s,int sp,int color,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(sp); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private TextView section(String s){ TextView t=txt(s,12,MUTED,true); t.setLetterSpacing(.12f); t.setPadding(dp(4),dp(24),0,dp(8)); return t; }
    private TextView pill(String s){ TextView t=txt(s,12,MUTED,true); t.setGravity(Gravity.CENTER); t.setBackground(round(CARD,999)); return t; }

    private Button remoteButton(String label,int bg,int cmd,int sp){
        Button b=new Button(this); b.setText(label); b.setTextColor(bg==ACCENT?BG:TEXT); b.setTextSize(sp);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackground(round(bg,22));
        b.setOnClickListener(v->{ v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); transmit(cmd); }); return b;
    }

    private Button action(String s,int bg,int fg){
        Button b=new Button(this); b.setText(s); b.setTextColor(fg); b.setTextSize(14); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackground(round(bg,14)); return b;
    }

    private GradientDrawable round(int color,int radius){
        GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }

    private View space(int w,int h){ View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(w,h)); return v; }
    private LinearLayout.LayoutParams square(int s){ return new LinearLayout.LayoutParams(dp(s),dp(s)); }
    private LinearLayout.LayoutParams weight(int h){ return new LinearLayout.LayoutParams(0,dp(h),1); }
    private View top(View v,int top){ LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(54)); lp.topMargin=dp(top); v.setLayoutParams(lp); return v; }
    private int dp(int v){ return Math.round(v*getResources().getDisplayMetrics().density); }
    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }

    private static class HidKey { final byte mod,code; HidKey(byte m,byte c){mod=m;code=c;} }
}
