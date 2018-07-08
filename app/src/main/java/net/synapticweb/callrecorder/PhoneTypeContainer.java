package net.synapticweb.callrecorder;

public class PhoneTypeContainer {

    private int typeCode;
    private String typeName;

    PhoneTypeContainer(int code, String name)
    {
        typeCode = code;
        typeName = name;
    }

    @Override
    public String toString(){
        return typeName;
    }

    public int getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(int typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
