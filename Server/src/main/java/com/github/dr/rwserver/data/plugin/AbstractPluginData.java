package com.github.dr.rwserver.data.plugin;

import com.github.dr.rwserver.io.ReusableByteInStream;
import com.github.dr.rwserver.struct.ObjectMap;
import com.github.dr.rwserver.struct.SerializerTypeAll;
import com.github.dr.rwserver.util.file.FileUtil;
import com.github.dr.rwserver.util.log.Log;
import com.github.dr.rwserver.util.zip.gzip.GzipDecoder;
import com.github.dr.rwserver.util.zip.gzip.GzipEncoder;

import java.io.*;

@SuppressWarnings("unchecked")
class AbstractPluginData {
    private static final ObjectMap<Class<?>, SerializerTypeAll.TypeSerializer<?>> serializers = new ObjectMap<>();
    private final ObjectMap<String, Value<?>> PLUGIN_DATA = new ObjectMap<>();
    private final ReusableByteInStream byteInputStream = new ReusableByteInStream();
    private final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    private final DataOutputStream dataOutput = new DataOutputStream(byteStream);
    private FileUtil fileUtil;

    static {
        DefaultSerializers.registrationBasis();
        DefaultSerializers.signUpForAdvanced();
    }

    public AbstractPluginData() {
    }

    public void setFileUtil(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    public <T> void setData(String name,T data) {
        PLUGIN_DATA.put(name,new Value<T>(data));
    }

    public <T> T getData(String name) {
        return (T) PLUGIN_DATA.get(name).getData();
    }

    public <T> T getData(String name,T data) {
        return (T) PLUGIN_DATA.get(name,new Value<T>(data)).getData();
    }

    public void read() {
        try (DataInputStream stream = new DataInputStream(GzipDecoder.getGzipInputStream(fileUtil.getInputsStream()))) {
            int amount = stream.readInt();

            for (int i = 0; i < amount; i++) {
                int length; byte[] bytes;
                String key = stream.readUTF();
                byte type = stream.readByte();
                switch (type) {
                    case 0:
                        PLUGIN_DATA.put(key, new Value<Boolean>(stream.readBoolean()));
                        break;
                    case 1:
                        PLUGIN_DATA.put(key, new Value<Integer>(stream.readInt()));
                        break;
                    case 2:
                        PLUGIN_DATA.put(key, new Value<Long>(stream.readLong()));
                        break;
                    case 3:
                        PLUGIN_DATA.put(key, new Value<Float>(stream.readFloat()));
                        break;
                    case 4:
                        PLUGIN_DATA.put(key, new Value<String>(stream.readUTF()));
                        break;
                    case 5:
                        /* 把String转为Class,来进行反序列化 */
                        Class<?> classCache = Class.forName(stream.readUTF());
                        length = stream.readInt();
                        bytes = new byte[length];
                        stream.read(bytes);
                        PLUGIN_DATA.put(key, new Value<>(getObject(classCache,bytes)));
                        break;
                }
            }
        } catch (Exception e) {
            Log.error("Read Data",e);
        }
    }

    public void save() {
        try(DataOutputStream stream = new DataOutputStream(GzipEncoder.getGzipOutputStream(fileUtil.writeByteOutputStream(false)))){
            stream.writeInt(PLUGIN_DATA.size);

            for(ObjectMap.Entry<String, Value<?>> entry : PLUGIN_DATA.entries()){
                stream.writeUTF(entry.key);
                Object value = entry.value.getData();
                if(value instanceof Boolean){
                    stream.writeByte(0);
                    stream.writeBoolean((Boolean)value);
                }else if(value instanceof Integer){
                    stream.writeByte(1);
                    stream.writeInt((Integer)value);
                }else if(value instanceof Long){
                    stream.writeByte(2);
                    stream.writeLong((Long)value);
                }else if(value instanceof Float){
                    stream.writeByte(3);
                    stream.writeFloat((Float)value);
                }else if(value instanceof String){
                    stream.writeByte(4);
                    stream.writeUTF((String)value);
                }else {
                    try {
                        byte[] bytes = putBytes(value);
                        stream.writeByte(5);
                        /* 去除ToString后的前缀(class com.xxx~) */
                        stream.writeUTF(value.getClass().toString().replace("class ",""));
                        stream.writeInt(bytes.length);
                        stream.write(bytes);
                    } catch (IOException e) {
                        Log.error("Save Error",e);
                    }
                }
            }
            stream.flush();
        }catch(Exception e){
            fileUtil.getFile().delete();
            Log.error("Write Data",e);
            throw new RuntimeException();
        }
    }

    protected static SerializerTypeAll.TypeSerializer getSerializer(Class type) {
        return serializers.get(type);
    }

    protected static <T> void setSerializer(Class<?> type, SerializerTypeAll.TypeSerializer<T> ser) {
        serializers.put(type, ser);
    }

    protected static <T> void setSerializer(Class<T> type, final SerializerTypeAll.TypeWriter<T> writer, final SerializerTypeAll.TypeReader<T> reader) {
        serializers.put(type, new SerializerTypeAll.TypeSerializer<T>() {
            @Override
            public void write(DataOutput stream, T object) throws IOException {
                writer.write(stream, object);
            }
            @Override
            public T read(DataInput stream) throws IOException {
                return reader.read(stream);
            }
        });
    }

    private  <T> T getObject(Class<T> type, byte[] bytes) {
        if (!serializers.containsKey(type)) {
            Log.error(new IllegalArgumentException("Type " + type + " does not have a serializer registered!"));
            return null;
        }
        SerializerTypeAll.TypeSerializer serializer = serializers.get(type);
        try {
            this.byteInputStream.setBytes(bytes);
            Object obj = serializer.read(new DataInputStream(byteInputStream));
            if (obj == null) {
                return null;
            }
            return (T)obj;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] putBytes(Object value) throws IOException {
        return putBytes(value, value.getClass());
    }

    private byte[] putBytes(Object value, Class<?> type) throws IOException {
        if (!serializers.containsKey(type)) {
            Log.error(new IllegalArgumentException("Type " + type + " does not have a serializer registered!"));
        }
        this.byteStream.reset();
        SerializerTypeAll.TypeSerializer<Object> serializer = (SerializerTypeAll.TypeSerializer)serializers.get(type);
        serializer.write(this.dataOutput, value);
        return this.byteStream.toByteArray();
    }
}