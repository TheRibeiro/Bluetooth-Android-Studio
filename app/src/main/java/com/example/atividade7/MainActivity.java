package com.example.atividade7;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private String texto;
    private byte[] entrada1 = new byte[100];
    private Thread processo;
    private String dataout;
    private TextView recebido;
    private Button ligabt;
    private Button conectabt;
    private Switch led1;
    private BluetoothAdapter ba;
    private Context context;
    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice blue_dev = null;
    private BluetoothSocket blue_soc = null;
    private int num;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela1);
        context = MainActivity.this;        // obtendo contexto

        // vinculando com as componentes da tela

        recebido = findViewById(R.id.recebido);
        ligabt = findViewById(R.id.ligar);
        conectabt = findViewById(R.id.conectar);
        led1 = findViewById(R.id.led1);

        // inicialmente desabilitando o botão de conectar e o botão do led
        //conectabt.setEnabled(false);
        //led1.setEnabled(false);

        // iniciando bluetoothadapter
        ba = BluetoothAdapter.getDefaultAdapter();

        // eventos dos botões

        ligabt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (ba.isEnabled() == false)   // se o bluetooth estiver desligado
                {
                    if (ba == null)  // se não existir bluetooth adapter
                    {
                        // exibe mensagem de erro
                        Toast.makeText(context, "Não existe dispositivo BT", Toast.LENGTH_LONG).show();
                        conectabt.setEnabled(false);   // desabilita o botão de conectar e o botão do led
                        led1.setEnabled(false);
                    } else if (!ba.isEnabled())  // se existir dispositivo bluetooth mas não estiver habilitado
                    {
                        Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        // inicia Activity para resultado com código 10;
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                          return;
                        }
                        startActivityForResult(mIntent, 10);  // código do processos é 10
                    }
                }
                else  // se ja estiver ligado
                {
                    if (ba != null) {
                        ba.disable();     // desliga o bluetooth
                        // exibe mensagem
                        Toast.makeText(context, "BT desligado", Toast.LENGTH_LONG).show();
                        conectabt.setEnabled(false);   // desabilita o botão de conectar e o botão do led
                        led1.setEnabled(false);
                    }
                }
            }
        });

        conectabt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // obtendo o endereço de máquina
                blue_dev = ba.getRemoteDevice("98:D3:32:70:C3:29");
                try {
                    // tentando uma conexão
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    blue_soc = blue_dev.createInsecureRfcommSocketToServiceRecord(myUUID);
                    blue_soc.connect();
                    led1.setEnabled(true);   // habilita o botão do led
                    // exibe mensagem
                    Toast.makeText(context,"BT conectado",Toast.LENGTH_LONG).show();
                }
                catch (Exception e)   // se der erro
                {
                    // exibe mensagem de erro
                    Toast.makeText(context,"Erro no BT",Toast.LENGTH_LONG).show();
                }
            }
        });


        led1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)     // se a chave estiver em true
                {
                    try {
                        dataout = "led1on";   // envia o comando para o Arduino
                        blue_soc.getOutputStream().write(dataout.getBytes());
                        // exibe mensagem
                        Toast.makeText(context, "Comando enviado", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        // exibe mensagem de erro
                        Toast.makeText(context, "Erro no BT", Toast.LENGTH_LONG).show();
                    }
                }
                else   // se a chave estiver em false
                {
                    try {
                        dataout = "led1off";   // envia o comando para o Arduino
                        blue_soc.getOutputStream().write(dataout.getBytes());
                        // exibe mensagem
                        Toast.makeText(context, "Comando enviado", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        // exibe mensagem de erro
                        Toast.makeText(context, "Erro no BT", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });


        processo=new Thread(new Runnable() {
            @Override
            public void run()
            {
                try
                {
                    while (true)   // loop infinito para verificar o recebimento de mensagens
                    {
                        Thread.sleep(100);   // espera 100ms
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                try
                                {
                                    if (blue_soc.getInputStream().available()!=0)   // se chegou algo pelo bluetooth
                                    {
                                        num=blue_soc.getInputStream().read(entrada1);           // lendo os bytes
                                        texto = new String(entrada1, 0, num);            // construindo String
                                        // exibindo a string
                                        recebido.setText(texto);
                                    }
                                }
                                catch (Exception e)
                                {

                                }
                            }
                        });
                    }
                }
                catch (Exception e)
                {

                }
            }
        });

        processo.start();   // iniciando a Thread
    }

    // método que obtem resultado da Activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) // se for o código 10 - ativação do dispositivo bluetooth
        {
            if (resultCode == RESULT_OK) // se o resultado estiver OK
            {
                // então o bluetooth deu certo
                // exibe mensagem
                Toast.makeText(context, "BT ligado", Toast.LENGTH_LONG).show();
                conectabt.setEnabled(true);   // habilita o botão de conectar

            } else   // do contrário
            {
                // exibe mensagem de erro
                Toast.makeText(context, "Erro na ativação do BT", Toast.LENGTH_LONG).show();
                conectabt.setEnabled(false);   // desabilita o botão de conectar e o botão do led
                led1.setEnabled(false);
            }
        }
    }
}