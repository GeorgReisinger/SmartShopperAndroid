package at.smartshopper.smartshopper.shoppinglist.details.item;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import java.util.List;



import at.smartshopper.smartshopper.R;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.MyViewHolder> {

    private List<at.smartshopper.smartshopper.shoppinglist.details.item.Item> data;

    public ItemAdapter(List<at.smartshopper.smartshopper.shoppinglist.details.item.Item> data){
        this.data = data;
    }


    /**
     * Erstellt einen Neuen view holder mit aktueller view
     * @param viewGroup
     * @param i
     * @return
     */
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardviewitem, viewGroup, false);

        MyViewHolder myViewHolder = new MyViewHolder(view);
        return myViewHolder;
    }

    /**
     * Setzt alle Daten in die View elemente
     * @param myViewHolder Das View Holder Objekt mit allen elementen
     * @param i Der Index welcher aus der data list genommen werden soll
     */
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {
        TextView itemName = myViewHolder.itemName;
        TextView itemAnzahl = myViewHolder.itemAnzahl;
        ImageView itemBearbeiten = myViewHolder.itemBearbeiten;
        ImageView itemDel = myViewHolder.itemDel;

        itemName.setText(data.get(i).getName());
        itemAnzahl.setText(data.get(i).getCount());
        Picasso.get().load(R.drawable.bearbeiten).into(itemBearbeiten);
        Picasso.get().load(R.drawable.delete).into(itemDel);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * Haltet alle elemente. Durch ein Objekt von dem kann jedes Element welches hier drinnen angeführt ist verwendet werden
     */
    public static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView itemName, itemAnzahl;
        ImageView itemBearbeiten, itemDel;

        public MyViewHolder(View itemView){
            super(itemView);

            this.itemName = (TextView)itemView.findViewById(R.id.nameItem);
            this.itemAnzahl = (TextView)itemView.findViewById(R.id.anzahlItem);
            this.itemBearbeiten = (ImageView)itemView.findViewById(R.id.itemBearbeiten);
            this.itemDel = (ImageView)itemView.findViewById(R.id.itemDel);
        }
    }
}