package com.barabd.facekiosk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.barabd.facekiosk.FaceKioskApp
import com.barabd.facekiosk.data.PersonEntity
import com.barabd.facekiosk.databinding.ActivityPeopleBinding
import com.barabd.facekiosk.databinding.ItemPersonBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PeopleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPeopleBinding
    private val app by lazy { application as FaceKioskApp }
    private val adapter = PeopleAdapter { person ->
        PinDialog.requirePin(this) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) { app.personRepository.delete(person.id) }
                reload()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPeopleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerPeople.layoutManager = LinearLayoutManager(this)
        binding.recyclerPeople.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        lifecycleScope.launch {
            val people = withContext(Dispatchers.IO) { app.personRepository.all() }
            adapter.submit(people)
        }
    }

    private class PeopleAdapter(
        private val onDelete: (PersonEntity) -> Unit
    ) : RecyclerView.Adapter<PeopleAdapter.VH>() {
        private var items: List<PersonEntity> = emptyList()

        fun submit(list: List<PersonEntity>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position], onDelete)
        }

        class VH(private val binding: ItemPersonBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(person: PersonEntity, onDelete: (PersonEntity) -> Unit) {
                binding.textName.text = person.displayName
                binding.textCode.text = person.attendanceCode
                binding.btnDelete.setOnClickListener { onDelete(person) }
            }
        }
    }
}
