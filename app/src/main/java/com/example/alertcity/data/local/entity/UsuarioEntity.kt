package com.example.alertcity.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.alertcity.domain.model.Usuario

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey
    @ColumnInfo(name = "idUsuario") val idUsuario: String,
    @ColumnInfo(name = "usuario") val usuario: String,
    @ColumnInfo(name = "contrasena") val contrasena: String,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "apellido") val apellido: String,
    @ColumnInfo(name = "direccion") val direccion: String
)

fun Usuario.toDomain(): Usuario = Usuario(
    idUsuario = idUsuario,
    usuario = usuario,
    contrasena = contrasena,
    nombre = nombre,
    apellido = apellido,
    direccion = direccion
)

fun UsuarioEntity.toEntity(): UsuarioEntity = UsuarioEntity(
    idUsuario = idUsuario,
    usuario = usuario,
    contrasena = contrasena,
    nombre = nombre,
    apellido = apellido,
    direccion = direccion
)