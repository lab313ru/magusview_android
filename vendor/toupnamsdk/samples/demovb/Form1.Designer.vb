<Global.Microsoft.VisualBasic.CompilerServices.DesignerGenerated()> _
Partial Class Form1
    Inherits System.Windows.Forms.Form

    'Form overrides dispose to clean up the component list.
    <System.Diagnostics.DebuggerNonUserCode()> _
    Protected Overrides Sub Dispose(ByVal disposing As Boolean)
        Try
            If disposing AndAlso components IsNot Nothing Then
                components.Dispose()
            End If
        Finally
            MyBase.Dispose(disposing)
        End Try
    End Sub

    'Required by the Windows Form Designer
    Private components As System.ComponentModel.IContainer

    'NOTE: The following procedure is required by the Windows Form Designer
    'It can be modified using the Windows Form Designer.  
    'Do not modify it using the code editor.
    <System.Diagnostics.DebuggerStepThrough()> _
    Private Sub InitializeComponent()
        Me.pictureBox1 = New System.Windows.Forms.PictureBox()
        Me.label1 = New System.Windows.Forms.Label()
        Me.trackBar1 = New System.Windows.Forms.TrackBar()
        Me.checkBox1 = New System.Windows.Forms.CheckBox()
        Me.button1 = New System.Windows.Forms.Button()
        Me.button2 = New System.Windows.Forms.Button()
        Me.CheckBox2 = New System.Windows.Forms.CheckBox()
        Me.Label2 = New System.Windows.Forms.Label()
        Me.TrackBar2 = New System.Windows.Forms.TrackBar()
        Me.Label3 = New System.Windows.Forms.Label()
        Me.TrackBar3 = New System.Windows.Forms.TrackBar()
        Me.Label4 = New System.Windows.Forms.Label()
        Me.TrackBar4 = New System.Windows.Forms.TrackBar()
        CType(Me.pictureBox1, System.ComponentModel.ISupportInitialize).BeginInit()
        CType(Me.trackBar1, System.ComponentModel.ISupportInitialize).BeginInit()
        CType(Me.TrackBar2, System.ComponentModel.ISupportInitialize).BeginInit()
        CType(Me.TrackBar3, System.ComponentModel.ISupportInitialize).BeginInit()
        CType(Me.TrackBar4, System.ComponentModel.ISupportInitialize).BeginInit()
        Me.SuspendLayout()
        '
        'pictureBox1
        '
        Me.pictureBox1.Location = New System.Drawing.Point(183, 10)
        Me.pictureBox1.Name = "pictureBox1"
        Me.pictureBox1.Size = New System.Drawing.Size(575, 487)
        Me.pictureBox1.SizeMode = System.Windows.Forms.PictureBoxSizeMode.Zoom
        Me.pictureBox1.TabIndex = 19
        Me.pictureBox1.TabStop = False
        '
        'label1
        '
        Me.label1.Location = New System.Drawing.Point(11, 115)
        Me.label1.Name = "label1"
        Me.label1.Size = New System.Drawing.Size(166, 12)
        Me.label1.TabIndex = 18
        Me.label1.Text = "Expo"
        Me.label1.TextAlign = System.Drawing.ContentAlignment.TopCenter
        '
        'trackBar1
        '
        Me.trackBar1.Location = New System.Drawing.Point(11, 140)
        Me.trackBar1.Name = "trackBar1"
        Me.trackBar1.Size = New System.Drawing.Size(166, 45)
        Me.trackBar1.TabIndex = 17
        Me.trackBar1.TickStyle = System.Windows.Forms.TickStyle.None
        '
        'checkBox1
        '
        Me.checkBox1.AutoSize = True
        Me.checkBox1.Location = New System.Drawing.Point(11, 79)
        Me.checkBox1.Name = "checkBox1"
        Me.checkBox1.Size = New System.Drawing.Size(102, 16)
        Me.checkBox1.TabIndex = 16
        Me.checkBox1.Text = "Auto Exposure"
        Me.checkBox1.UseVisualStyleBackColor = True
        '
        'button1
        '
        Me.button1.Location = New System.Drawing.Point(11, 10)
        Me.button1.Name = "button1"
        Me.button1.Size = New System.Drawing.Size(166, 23)
        Me.button1.TabIndex = 13
        Me.button1.Text = "Start"
        Me.button1.UseVisualStyleBackColor = True
        '
        'button2
        '
        Me.button2.Location = New System.Drawing.Point(11, 45)
        Me.button2.Name = "button2"
        Me.button2.Size = New System.Drawing.Size(166, 23)
        Me.button2.TabIndex = 21
        Me.button2.Text = "Capture"
        Me.button2.UseVisualStyleBackColor = True
        '
        'CheckBox2
        '
        Me.CheckBox2.AutoSize = True
        Me.CheckBox2.Location = New System.Drawing.Point(11, 208)
        Me.CheckBox2.Name = "CheckBox2"
        Me.CheckBox2.Size = New System.Drawing.Size(102, 16)
        Me.CheckBox2.TabIndex = 22
        Me.CheckBox2.Text = "White Balance"
        Me.CheckBox2.UseVisualStyleBackColor = True
        '
        'Label2
        '
        Me.Label2.Location = New System.Drawing.Point(11, 246)
        Me.Label2.Name = "Label2"
        Me.Label2.Size = New System.Drawing.Size(166, 12)
        Me.Label2.TabIndex = 24
        Me.Label2.Text = "R"
        Me.Label2.TextAlign = System.Drawing.ContentAlignment.TopCenter
        '
        'TrackBar2
        '
        Me.TrackBar2.Location = New System.Drawing.Point(11, 271)
        Me.TrackBar2.Name = "TrackBar2"
        Me.TrackBar2.Size = New System.Drawing.Size(166, 45)
        Me.TrackBar2.TabIndex = 23
        Me.TrackBar2.TickStyle = System.Windows.Forms.TickStyle.None
        '
        'Label3
        '
        Me.Label3.Location = New System.Drawing.Point(11, 325)
        Me.Label3.Name = "Label3"
        Me.Label3.Size = New System.Drawing.Size(166, 12)
        Me.Label3.TabIndex = 26
        Me.Label3.Text = "G"
        Me.Label3.TextAlign = System.Drawing.ContentAlignment.TopCenter
        '
        'TrackBar3
        '
        Me.TrackBar3.Location = New System.Drawing.Point(11, 350)
        Me.TrackBar3.Name = "TrackBar3"
        Me.TrackBar3.Size = New System.Drawing.Size(166, 45)
        Me.TrackBar3.TabIndex = 25
        Me.TrackBar3.TickStyle = System.Windows.Forms.TickStyle.None
        '
        'Label4
        '
        Me.Label4.Location = New System.Drawing.Point(11, 405)
        Me.Label4.Name = "Label4"
        Me.Label4.Size = New System.Drawing.Size(166, 12)
        Me.Label4.TabIndex = 28
        Me.Label4.Text = "B"
        Me.Label4.TextAlign = System.Drawing.ContentAlignment.TopCenter
        '
        'TrackBar4
        '
        Me.TrackBar4.Location = New System.Drawing.Point(11, 430)
        Me.TrackBar4.Name = "TrackBar4"
        Me.TrackBar4.Size = New System.Drawing.Size(166, 45)
        Me.TrackBar4.TabIndex = 27
        Me.TrackBar4.TickStyle = System.Windows.Forms.TickStyle.None
        '
        'Form1
        '
        Me.AutoScaleDimensions = New System.Drawing.SizeF(6.0!, 12.0!)
        Me.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font
        Me.ClientSize = New System.Drawing.Size(770, 509)
        Me.Controls.Add(Me.Label4)
        Me.Controls.Add(Me.TrackBar4)
        Me.Controls.Add(Me.Label3)
        Me.Controls.Add(Me.TrackBar3)
        Me.Controls.Add(Me.Label2)
        Me.Controls.Add(Me.TrackBar2)
        Me.Controls.Add(Me.CheckBox2)
        Me.Controls.Add(Me.button2)
        Me.Controls.Add(Me.pictureBox1)
        Me.Controls.Add(Me.label1)
        Me.Controls.Add(Me.trackBar1)
        Me.Controls.Add(Me.checkBox1)
        Me.Controls.Add(Me.button1)
        Me.Name = "Form1"
        Me.Text = "demovb"
        CType(Me.pictureBox1, System.ComponentModel.ISupportInitialize).EndInit()
        CType(Me.trackBar1, System.ComponentModel.ISupportInitialize).EndInit()
        CType(Me.TrackBar2, System.ComponentModel.ISupportInitialize).EndInit()
        CType(Me.TrackBar3, System.ComponentModel.ISupportInitialize).EndInit()
        CType(Me.TrackBar4, System.ComponentModel.ISupportInitialize).EndInit()
        Me.ResumeLayout(False)
        Me.PerformLayout()

    End Sub
    Private WithEvents pictureBox1 As System.Windows.Forms.PictureBox
    Private WithEvents label1 As System.Windows.Forms.Label
    Private WithEvents trackBar1 As System.Windows.Forms.TrackBar
    Private WithEvents checkBox1 As System.Windows.Forms.CheckBox
    Private WithEvents button1 As System.Windows.Forms.Button
    Private WithEvents button2 As Button
    Friend WithEvents CheckBox2 As CheckBox
    Private WithEvents Label2 As Label
    Private WithEvents TrackBar2 As TrackBar
    Private WithEvents Label3 As Label
    Private WithEvents TrackBar3 As TrackBar
    Private WithEvents Label4 As Label
    Private WithEvents TrackBar4 As TrackBar
End Class
