package br.edu.uniceplac.modapraia.data

import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate

/** Carga explícita e atômica para apresentação. Não altera registros preexistentes. */
internal object DemoData {
    fun insert(db: SQLiteDatabase) {
        val loaded=db.rawQuery("SELECT 1 FROM products WHERE id='DEMO-p1'",null).use {it.moveToFirst()}
        if(loaded) return
        val today=LocalDate.now()
        fun day(offset: Long)=today.plusDays(offset).toString()
        fun timestamp(offset: Long)=day(offset)+"T10:30:00"
        val names=listOf("Biquíni Aurora (demo)","Maiô Sol (demo)","Saída Brisa (demo)","Canga Mar (demo)","Biquíni Coral (demo)","Chapéu Verão (demo)")
        val prices=listOf(15990L,18990L,21990L,7990L,14990L,6990L)
        val sizes=listOf("M","G","Único","Único","P","Único")
        val colors=listOf("Verde","Preto","Branco","Azul","Coral","Palha")
        val quantities=listOf(20,12,15,28,2,18)
        names.forEachIndexed { index,name ->
            val id="DEMO-p${index+1}"
            db.execSQL("INSERT INTO products VALUES (?,?,?,?,?,?,?,?,?)",arrayOf(id,name,if(index<2 || index==4)"Moda praia" else "Acessórios",sizes[index],colors[index],"DEMO-${index+1}",quantities[index],if(index==4)4 else 3,prices[index]/100.0))
            db.execSQL("INSERT INTO movements VALUES (?,?,?,?,?,?,?)",arrayOf("DEMO-in${index+1}",id,name,"ENTRY",quantities[index],timestamp(-7),"Estoque inicial de demonstração"))
        }
        fun sale(number: Int, product: Int, quantity: Int, offset: Long, due: Long, received: Long) {
            val total=prices[product-1]*quantity
            db.execSQL("UPDATE products SET quantity=quantity-? WHERE id=?",arrayOf(quantity,"DEMO-p$product"))
            db.execSQL("INSERT INTO sales VALUES (?,?,?,?,?,?,?,0)",arrayOf("DEMO-s$number","DEMO-p$product",names[product-1],"Cliente fictícia $number",quantity,total,timestamp(offset)))
            db.execSQL("INSERT INTO movements VALUES (?,?,?,?,?,?,?)",arrayOf("DEMO-out$number","DEMO-p$product",names[product-1],"SALE",quantity,timestamp(offset),"Venda de demonstração $number"))
            db.execSQL("INSERT INTO bills VALUES (?,?,?,?,?,?,?,0)",arrayOf("DEMO-r$number","RECEIVE","Venda demo $number: ${names[product-1]}","Cliente fictícia $number",total,day(due),"DEMO-s$number"))
            if(received>0) db.execSQL("INSERT INTO payments VALUES (?,?,?,?)",arrayOf("DEMO-received$number","DEMO-r$number",received,timestamp(offset)))
        }
        sale(1,1,3,-6,-6,47970)
        sale(2,2,2,-4,-4,20000)
        sale(3,3,1,-3,5,0)
        sale(4,4,4,-2,-2,31960)
        sale(5,6,2,-1,7,6990)
        fun payable(id: Int,title: String,total: Long,due: Long,paid: Long,offset: Long) {
            db.execSQL("INSERT INTO bills VALUES (?,?,?,?,?,?,NULL,0)",arrayOf("DEMO-pay$id","PAY","$title (demo)","Fornecedor fictício $id",total,day(due)))
            if(paid>0) db.execSQL("INSERT INTO payments VALUES (?,?,?,?)",arrayOf("DEMO-paid$id","DEMO-pay$id",paid,timestamp(offset)))
        }
        payable(1,"Reposição de peças",45000,4,15000,-5)
        payable(2,"Embalagens",12500,-2,0,-2)
        payable(3,"Divulgação",9000,-3,9000,-3)
        payable(4,"Frete de fornecedor",6500,8,0,0)
        db.execSQL("INSERT INTO orders VALUES (?,?,?,?,?,?,?,?,?,'Aberta',NULL)",arrayOf("DEMO-o1","DEMO-p1",names[0],"Marina fictícia","Contato de demonstração",2,31980,day(3),"Retirada na loja; embalagem para presente. Exemplo fictício."))
        db.execSQL("INSERT INTO orders VALUES (?,?,?,?,?,?,?,?,?,'Em preparação',NULL)",arrayOf("DEMO-o2","DEMO-p3",names[2],"Paula fictícia","Contato de demonstração",1,21990,day(1),"Separar a peça branca. Exemplo fictício."))
        db.execSQL("INSERT INTO orders VALUES (?,?,?,?,?,?,?,?,?,'Cancelada',NULL)",arrayOf("DEMO-o3","DEMO-p2",names[1],"Luiza fictícia","Contato de demonstração",1,18990,day(-1),"Exemplo de encomenda cancelada sem baixa de estoque."))
    }
}
