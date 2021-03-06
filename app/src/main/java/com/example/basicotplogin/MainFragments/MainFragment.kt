package com.example.basicotplogin.MainFragments

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.basicotplogin.AdapterClasses.PostAdapter
import com.example.basicotplogin.ComparatorClass
import com.example.basicotplogin.CreatePost
import com.example.basicotplogin.MessageChatActivity
import com.example.basicotplogin.ModelClasses.EditBroadCast
import com.example.basicotplogin.ModelClasses.Posts
import com.example.basicotplogin.ModelClasses.Users
import com.example.basicotplogin.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainFragment : Fragment() {

    var userIdVisit: String = ""
    var firebaseUser: FirebaseUser? = null
    var reference: DatabaseReference? = null
    private var mUserChats: List<Posts>? = null
    private var userAdapter: PostAdapter? = null
    private var recylerView: RecyclerView? = null
    var Admin: Boolean = false
    var refAdmin: DatabaseReference? = null
    var firebaseUserAdmin: String = ""
    var my_dict: HashMap<String, ArrayList<String>> = HashMap()
    private var progressBar: ProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.fragment_main, container, false)

        progressBar = ProgressDialog(context)
        progressBar!!.setMessage("Almost There, Please wait...")
        progressBar!!.show()

        recylerView = view.findViewById(R.id.recycler_view_post)
        recylerView!!.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(view.context)
        recylerView!!.layoutManager = linearLayoutManager

        val fab: FloatingActionButton = view.findViewById(R.id.fab)

        //get all broadcasts and childs
        var refBroadcast = FirebaseDatabase.getInstance().reference.child("BroadCastDetails")
        refBroadcast.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(BroadCastName : DataSnapshot) {
                for (SingleName in BroadCastName.children){
                    var my_array: ArrayList<String> = ArrayList()
                    for (userId in SingleName.children){
                        val Id : EditBroadCast = userId.getValue(EditBroadCast::class.java)!!
                        my_array.add(Id.getID().toString())
                    }
                    my_dict.put(SingleName.key!!, my_array)
                }

                Log.i("\n\n\nList : ", my_dict.toString())

                //check admin

                refAdmin = FirebaseDatabase.getInstance().reference.child("Admin")
                refAdmin!!.addListenerForSingleValueEvent( object : ValueEventListener {
                    override fun onDataChange(p0: DataSnapshot) {
                        if(p0.exists()) {
                            val user: Users? = p0.getValue(Users::class.java)       //create user of instance Users class
                            firebaseUserAdmin = user!!.getUID()!!

                            if (FirebaseAuth.getInstance().currentUser!!.uid.equals(firebaseUserAdmin)) {
                                Admin = true
                                view.create_post_btn.visibility = View.VISIBLE

                                fab.visibility = View.GONE

                                view.create_post_btn.setOnClickListener {
                                    val intent =  Intent(context, CreatePost::class.java)
                                    intent.putExtra("dict", my_dict)
                                    Log.i("Dictionary Send", "success ")
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                }

                            }
                            else{
                                view.create_post_btn.visibility = View.GONE  //GONE

                                fab.setOnClickListener {
                                    val intent =  Intent(context, MessageChatActivity::class.java)
                                    intent.putExtra("visit_id", firebaseUserAdmin)
                                    context!!.startActivity(intent)
                                }

                            }
                        }
                        mUserChats = ArrayList()
                        retrieveAllPosts()
                    }

                    override fun onCancelled(p0: DatabaseError) {}
                })

            }

            override fun onCancelled(BroadCastName: DatabaseError) {}
        })

        return view
    }

    private fun retrieveAllPosts() {

        //val firebaseUserAdmin = "y33x7HMMKMRIIJn4XTI5YH1Xzv12"

        if(Admin){
            val refUsers = FirebaseDatabase.getInstance().reference.child("Posts")  //get all users ids

            refUsers.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    (mUserChats as ArrayList<Posts>).clear()
                    //Toast.makeText(context, "Retriving all", Toast.LENGTH_LONG).show()
                    for (snapshotParent in p0.children){
                        for (snapshot in snapshotParent.children) {
                            val user: Posts? = snapshot.getValue(Posts::class.java)
                            (mUserChats as ArrayList<Posts>).add(user!!)
                        }
                    }
                    try {
                        //Compare with respect to date of upload
                        var MUserChats : MutableList<Posts> = ArrayList(mUserChats!!)
                        Collections.sort(MUserChats, ComparatorClass())
                        Collections.reverse(MUserChats)


                        userAdapter = PostAdapter(view!!.context, MUserChats, Admin)
                        recylerView!!.isNestedScrollingEnabled = false
                        recylerView!!.adapter = userAdapter
                        progressBar!!.dismiss()
                    }
                    catch (e: Exception) {

                    }

                }

                override fun onCancelled(p0: DatabaseError) {

                }
            })
        }
        else{
            val refUsers = FirebaseDatabase.getInstance().reference.child("Posts")  //get all users ids

            refUsers.addValueEventListener(object: ValueEventListener {
                override fun onDataChange(p0: DataSnapshot) {
                    (mUserChats as ArrayList<Posts>).clear()
                    //Toast.makeText(context, "Retriving Admin Only", Toast.LENGTH_LONG).show()
                    for (snapshotParent in p0.children){
                        if(firebaseUserAdmin.equals(snapshotParent.key) or FirebaseAuth.getInstance().currentUser!!.uid.equals(snapshotParent.key)){
                            for (snapshot in snapshotParent.children) {
                                val user: Posts? = snapshot.getValue(Posts::class.java)

                                if(user!!.getGroup().toString().equals("public")){
                                    (mUserChats as ArrayList<Posts>).add(user)
                                }
                                else {
                                    if(FirebaseAuth.getInstance().currentUser!!.uid.equals(user.getSenderId().toString())){
                                        (mUserChats as ArrayList<Posts>).add(user)
                                    }
                                    else{
                                        var temp = user.getGroup().toString()
                                        var my_users = my_dict[temp]
                                        var my_id = FirebaseAuth.getInstance().currentUser!!.uid
                                        if(my_users!!.contains(my_id)){
                                            (mUserChats as ArrayList<Posts>).add(user)
                                        }
                                    }
                                }

                            }
                        }

                    }
                    try {
                        var MUserChats : MutableList<Posts> = ArrayList(mUserChats!!)
                        Collections.sort(MUserChats, ComparatorClass())
                        Collections.reverse(MUserChats)

                        userAdapter = PostAdapter(view!!.context, MUserChats, Admin)
                        recylerView!!.isNestedScrollingEnabled = false
                        recylerView!!.adapter = userAdapter
                        progressBar!!.dismiss()
                    }
                    catch (e: Exception) {}

                }

                override fun onCancelled(p0: DatabaseError) {}

            })
        }



    }

}